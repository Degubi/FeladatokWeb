package degubi.services;

import static degubi.Main.*;

import degubi.model.task.*;
import degubi.model.user.*;
import degubi.repositories.tasks.*;
import degubi.repositories.users.*;
import degubi.utils.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.Map.*;
import java.util.regex.*;
import java.util.stream.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/tasks")
@RestController
@CrossOrigin("http://localhost:8081")
public final class TasksService {
    private static final Pattern classNamePattern = Pattern.compile("class ([^\\s\\\\]*)[ ]*\\{");
    private static final Pattern taskPattern = Pattern.compile("([0-9]*)\\. feladat:(.+?)(?=[0-9]*\\. feladat)", Pattern.CASE_INSENSITIVE);
    private static final Pattern newLinePattern = Pattern.compile("\n|\r\n");

    private final TasksRepository tasks;
    private final UserRepository users;

    public TasksService(TasksRepository tasks, UserRepository users) {
        this.tasks = tasks;
        this.users = users;
    }

    @GetMapping("/list")
    public Map<String, TaskListResponse[]> listTasks(@RequestHeader String userID) {
        var userTaskStatuses = userID.equals("null") ? new ArrayList<UserTaskStatus>() : users.findById(UUID.fromString(userID)).taskStatuses;

        return tasks.perCategoryTasks().entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, e -> createTaskResponse(e.getValue(), userTaskStatuses), (k, l) -> l, LinkedHashMap::new));
    }

    private static TaskListResponse[] createTaskResponse(Task[] tasks, List<UserTaskStatus> userTaskStatuses) {
        return Arrays.stream(tasks)
                     .map(k -> new TaskListResponse(k.ID, k.name, k.year, k.month, k.subtaskCount, getCompletedSubtaskCount(k.ID, userTaskStatuses), k.pdfPath, k.resourceFileEntryNames, k.solutionFilePathsPerExtension))
                     .toArray(TaskListResponse[]::new);
    }

    private static int getCompletedSubtaskCount(String taskID, List<UserTaskStatus> userTaskStatuses) {
        return userTaskStatuses.stream()
                               .filter(k -> k.taskId.equals(taskID))
                               .findFirst()
                               .map(k -> k.completedSubtasks)
                               .orElse(0);
    }

    @SuppressWarnings("boxing")
    @PostMapping("/{taskID}/java")
    public ResponseEntity<TestResponse> handleJavaTaskTest(@RequestBody String userSource, @PathVariable String taskID, @RequestHeader String userID) throws IOException {
        var workDirName = "java_" + System.currentTimeMillis();
        var classNameMatcher = classNamePattern.matcher(userSource);
        var userIDToSend = userID.equals("null") ? users.save(new User(null, new ArrayList<>(0))).id.toString() : userID;

        if(!classNameMatcher.find()) {
            return new ResponseEntity<>(new TestResponse("Nem található class név a bemeneti fájlban!", "Sikertelen tesztelés!", userIDToSend), HttpStatus.BAD_REQUEST);
        }

        var taskMeta = tasks.perIDTasks().get(taskID);
        if(taskMeta == null) {
            return new ResponseEntity<>(new TestResponse("Nem található adat ehhez a feladathoz: " + taskID, "Sikertelen tesztelés!", userIDToSend), HttpStatus.NOT_FOUND);
        }

        var userClassName = classNameMatcher.group(1);
        var workDirPath = Files.createDirectory(Path.of(workDirName));
        var userSourceFileName = userClassName + ".java";

        Files.writeString(Path.of(workDirPath + "/" + userSourceFileName), userSource);

        var compilationOutput = compileUserSource(workDirName, "javac -encoding utf8 " + userSourceFileName);
        if(!compilationOutput.isEmpty()) {
            IOUtils.deleteDirectory(workDirPath);
            return new ResponseEntity<>(new TestResponse("Fordítási hiba történt: \n" + compilationOutput, "Sikertelen tesztelés!", userIDToSend), HttpStatus.OK);
        }

        extractResourceFiles(taskID, workDirPath);

        var testConsoleOutput = runUserSource(workDirName, taskMeta.consoleInput, "java -Dfile.encoding=UTF-8 " + userClassName);
        if(testConsoleOutput.startsWith("Futtatási hiba") || testConsoleOutput.startsWith("A tesztelés időtúllépés")) {
            IOUtils.deleteDirectory(workDirPath);
            return new ResponseEntity<>(new TestResponse(testConsoleOutput, "Sikertelen tesztelés!", userIDToSend), HttpStatus.OK);
        }

        // Add dummy '999. feladat:' at the end so that the last task output is not skipped
        var formattedConsoleOutput = newLinePattern.matcher(testConsoleOutput).replaceAll(";") + ";999. feladat:";
        var consoleOutputsPerTask = taskPattern.matcher(formattedConsoleOutput).results()
                                               .collect(Collectors.toMap(k -> Integer.parseInt(k.group(1)), k -> k.group(2)));

        var outputTestOutputLines = Arrays.stream(taskMeta.solutions)
                                          .map(k -> runTestForTask(k, consoleOutputsPerTask.get(k.taskOrdinal), workDirPath))
                                          .toArray(String[]::new);

        IOUtils.deleteDirectory(workDirPath);

        var correctTasksCount = Arrays.stream(outputTestOutputLines)
                                      .filter(k -> k.endsWith(". feladat: helyes"))
                                      .mapToInt(k -> 1)
                                      .sum();

        var userData = users.findById(UUID.fromString(userIDToSend));
        userData.taskStatuses.removeIf(k -> k.taskId.equals(taskID));
        userData.taskStatuses.add(new UserTaskStatus(null, taskID, correctTasksCount, userData));
        users.save(userData);

        return new ResponseEntity<>(new TestResponse(testConsoleOutput, String.join("\n", outputTestOutputLines), userIDToSend), HttpStatus.OK);
    }


    private static void extractResourceFiles(String taskID, Path workDirPath) throws IOException {
        try(var resourceFilesForTask = Files.list(Path.of(TASK_CACHE_FOLDER + "/" + taskID + "/resources"))) {
            resourceFilesForTask.forEach(k -> IOUtils.copyFile(k, Path.of(workDirPath + "/" + k.getFileName())));
        }
    }

    private static String runTestForTask(ParsedSolution solution, String userTaskOutput, Path workDirPath) {
        return solution.missingSolutionChecker.test(userTaskOutput, workDirPath) ? solution.taskOrdinal + ". feladat: Nem található kimenet ehhez a feladathoz" :
               solution.wrongnessChecker.test(userTaskOutput, workDirPath) ? solution.wrongSolutionMessage
                                                                           : solution.taskOrdinal + ". feladat: helyes";
    }

    @SuppressWarnings("resource")
    private static String runUserSource(String workDirName, String[] consoleInputLines, String command) throws IOException {
        var testProcess = Runtime.getRuntime().exec(command, new String[0], Path.of(workDirName).toAbsolutePath().toFile());
        var processStdin = testProcess.getOutputStream();
        processStdin.write(String.join("\n", consoleInputLines).getBytes());
        processStdin.flush();

        var completed = IOUtils.runProcessWithTimeout(testProcess);
        if(!completed) {
            return "A tesztelés időtúllépés miatt leállt!";
        }

        var optionalErrorOutput = IOUtils.getProcessOutputFrom(testProcess.getErrorStream());
        if(!optionalErrorOutput.isEmpty()) {
            return "Futtatási hiba történt: \n" + optionalErrorOutput;
        }

        return IOUtils.getProcessOutputFrom(testProcess.getInputStream());
    }

    @SuppressWarnings("resource")
    private static String compileUserSource(String workDirName, String command) throws IOException {
        var compileProcess = Runtime.getRuntime().exec(command, new String[0], Path.of(workDirName).toAbsolutePath().toFile());
        var completed = IOUtils.runProcessWithTimeout(compileProcess);

        return completed ? IOUtils.getProcessOutputFrom(compileProcess.getErrorStream()) : "A fordítás időtúllépés miatt leállt!";
    }
}