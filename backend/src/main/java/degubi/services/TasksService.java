package degubi.services;

import static degubi.Main.*;

import degubi.model.*;
import degubi.repositories.*;
import degubi.utils.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
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

    public TasksService(TasksRepository tasks) {
        this.tasks = tasks;
    }

    @GetMapping
    public Map<String, List<Task>> listTasks() {
        return tasks.perCategory;
    }

    @SuppressWarnings("boxing")
    @PostMapping("/{taskID}/java")
    public ResponseEntity<String[]> handleJavaTaskTest(@RequestBody String userSource, @PathVariable String taskID) throws IOException {
        var workDirName = "java_" + System.currentTimeMillis();
        var classNameMatcher = classNamePattern.matcher(userSource);

        if(!classNameMatcher.find()) {
            return new ResponseEntity<>(new String[]{ "Nem található class név a bemeneti fájlban!", "Sikertelen tesztelés!" }, HttpStatus.BAD_REQUEST);
        }

        var taskMeta = tasks.perID.get(taskID);
        if(taskMeta == null) {
            return new ResponseEntity<>(new String[]{ "Nem található adat ehhez a feladathoz: " + taskID, "Sikertelen tesztelés!" }, HttpStatus.NOT_FOUND);
        }

        var className = classNameMatcher.group(1);
        var workDirPath = Files.createDirectory(Path.of(workDirName));
        var userSourceFilePath = className + ".java";

        Files.writeString(Path.of(workDirPath + "/" + userSourceFilePath), userSource);

        var compileOutput = getCompileOutput(workDirName, "javac -encoding utf8 " + userSourceFilePath);
        if(!compileOutput.isEmpty()) {
            IOUtils.deleteDirectory(workDirPath);
            return new ResponseEntity<>(new String[]{ "Fordítási hiba történt: \n" + compileOutput, "Sikertelen tesztelés!" }, HttpStatus.OK);
        }

        extractResourceFiles(taskID, workDirPath);

        var testConsoleOutput = getTestOutput(workDirName, taskMeta.consoleInput, "java -Dfile.encoding=UTF-8 " + className);
        if(testConsoleOutput.startsWith("Futtatási hiba") || testConsoleOutput.startsWith("A tesztelés időtúllépés")) {
            IOUtils.deleteDirectory(workDirPath);
            return new ResponseEntity<>(new String[]{ testConsoleOutput, "Sikertelen tesztelés!" }, HttpStatus.OK);
        }

        // Add dummy '999. feladat:' at the end so that the last task output is not skipped
        var formattedConsoleOutput = newLinePattern.matcher(testConsoleOutput).replaceAll(";") + ";999. feladat:";
        var consoleOutputsPerTask = taskPattern.matcher(formattedConsoleOutput).results()
                                               .collect(Collectors.toMap(k -> Integer.parseInt(k.group(1)), k -> k.group(2)));

        var outputTestOutput = Arrays.stream(taskMeta.solutions)
                                     .map(k -> runTestForTask(k, consoleOutputsPerTask.get(k.taskOrdinal), workDirPath))
                                     .collect(Collectors.joining("\n"));

        IOUtils.deleteDirectory(workDirPath);
        return new ResponseEntity<>(new String[]{ testConsoleOutput, outputTestOutput }, HttpStatus.OK);
    }


    private static void extractResourceFiles(String taskID, Path workDirPath) throws IOException {
        try(var resourceFilesForTask = Files.list(Path.of(CACHE_FOLDER + "/" + taskID + "/resources"))) {
            resourceFilesForTask.forEach(k -> IOUtils.copyFile(k, Path.of(workDirPath + "/" + k.getFileName())));
        }
    }

    private static String runTestForTask(ParsedSolution solution, String userTaskOutput, Path workDirPath) {
        return solution.missingSolutionChecker.test(userTaskOutput, workDirPath) ? solution.taskOrdinal + ". feladat: Nem található kimenet ehhez a feladathoz" :
               solution.wrongnessChecker.test(userTaskOutput, workDirPath) ? solution.wrongSolutionMessage
                                                                           : solution.taskOrdinal + ". feladat: helyes";
    }

    @SuppressWarnings("resource")
    private static String getTestOutput(String workDirName, String[] consoleInputLines, String command) throws IOException {
        var testProcess = Runtime.getRuntime().exec(command, null, Path.of(workDirName).toAbsolutePath().toFile());
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
    private static String getCompileOutput(String workDirName, String command) throws IOException {
        var compileProcess = Runtime.getRuntime().exec(command, null, Path.of(workDirName).toAbsolutePath().toFile());
        var completed = IOUtils.runProcessWithTimeout(compileProcess);

        return completed ? IOUtils.getProcessOutputFrom(compileProcess.getErrorStream()) : "A fordítás időtúllépés miatt leállt!";
    }
}