package degubi.repositories;

import static degubi.Main.*;

import degubi.model.*;
import degubi.utils.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.springframework.boot.web.client.*;
import org.springframework.stereotype.*;

@Repository
public final class TasksRepository {

    public final Map<String, List<Task>> perCategory;
    public final Map<String, Task> perID;

    public TasksRepository(RestTemplateBuilder templateBuilder) {
        System.out.println("Initializing task cache");

        var localZipPath = Path.of("Feladatok-master.zip");
        var zipBytes = Files.exists(localZipPath) ? IOUtils.readAllBytesFromFile(localZipPath)
                                                  : templateBuilder.build().getForObject("https://github.com/Degubi/Feladatok/archive/master.zip", byte[].class);

        perCategory = createPerCategoryMap(zipBytes);
        perID = perCategory.values().stream()
                           .flatMap(List::stream)
                           .collect(Collectors.toMap(k -> k.ID, k -> k));

        System.out.println("Task cache initialization done");
    }


    private static LinkedHashMap<String, List<Task>> createPerCategoryMap(byte[] zipBytes) {
        var zipEntries = IOUtils.getAllZipEntryNamesTruncated(zipBytes);
        var folders = zipEntries.get(Boolean.TRUE);
        var files = zipEntries.get(Boolean.FALSE);

        var result = new LinkedHashMap<String, List<Task>>();
        result.put("Emelt Érettségi", getTasksForCategory("erettsegi_emelt", folders, files, zipBytes));
        result.put("Emelt Informatika Ismeretek", getTasksForCategory("informatika_ismeretek/emelt", folders, files, zipBytes));
        result.put("Közép Informatika Ismeretek", getTasksForCategory("informatika_ismeretek/kozep", folders, files, zipBytes));
        result.put("OKJ Rendszerüzemeltető", getTasksForCategory("okj/rendszeruzemelteto", folders, files, zipBytes));
        result.put("OKJ Szoftverfejlesztő", getTasksForCategory("okj/szoftverfejleszto", folders, files, zipBytes));
        return result;
    }

    private static List<Task> getTasksForCategory(String categoryFolderPrefix, List<String> folders, List<String> files, byte[] zipBytes) {
        var taskTopLevelFolderSlashCount = categoryFolderPrefix.chars().filter(k -> k == '/').count() + 2;

        return folders.stream()
                      .filter(k -> k.startsWith(categoryFolderPrefix) && k.chars().filter(l -> l == '/').count() == taskTopLevelFolderSlashCount)
                      .map(k -> k.substring(categoryFolderPrefix.length() + 1, k.length() - 1))
                      .map(taskFolderName -> new Task(taskFolderName, getFilesPerExtensionForTask(files, categoryFolderPrefix + '/' + taskFolderName), zipBytes))
                      .peek(k -> extractTaskResourceFiles(k, zipBytes))
                      .sorted(Comparator.<Task>comparingInt(k -> k.year).reversed())
                      .collect(Collectors.toList());
    }

    private static Map<String, List<String>> getFilesPerExtensionForTask(List<String> files, String fileNamePrefix) {
        return files.stream()
                    .filter(k -> k.startsWith(fileNamePrefix))
                    .collect(Collectors.groupingBy(k -> k.substring(k.indexOf('.'))));
    }

    private static void extractTaskResourceFiles(Task task, byte[] zipBytes) {
        try {
            var taskResourcesCacheFolder = Files.createDirectories(Path.of(TASK_CACHE_FOLDER + "/" + task.ID + "/resources"));
            var taskSolutionsCacheFolder = Files.createDirectory(Path.of(TASK_CACHE_FOLDER + "/" + task.ID + "/solutions"));

            Arrays.stream(task.resourceFileEntryNames)
                  .forEach(k -> IOUtils.extractZipEntry(taskResourcesCacheFolder, k, zipBytes));

            Arrays.stream(task.solutions)
                  .map(k -> k.optionalFileToDownload)
                  .filter(k -> k.startsWith("https"))
                  .forEach(k -> IOUtils.extractZipEntry(taskSolutionsCacheFolder, k, zipBytes));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}