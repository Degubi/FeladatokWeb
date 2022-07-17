package degubi.repositories;

import static degubi.Main.*;

import degubi.*;
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

        var zipBytes = Main.ENV == Main.DEV_ENV ? readAllBytesFromFile(Path.of("Feladatok-master.zip"))
                                                : templateBuilder.build().getForObject("https://github.com/Degubi/Feladatok/archive/master.zip", byte[].class);

        perCategory = createPerCategoryMap(zipBytes);
        perID = perCategory.values().stream()
                           .flatMap(List::stream)
                           .collect(Collectors.toMap(k -> k.ID, k -> k));

        System.out.println("Task cache initialization done");
    }


    private static LinkedHashMap<String, List<Task>> createPerCategoryMap(byte[] zipBytes) {
        var result = new LinkedHashMap<String, List<Task>>();
        var zipEntries = IOUtils.getAllZipEntryNamesTruncated(zipBytes);
        var folders = zipEntries.get(Boolean.TRUE);
        var files = zipEntries.get(Boolean.FALSE);

        result.put("Emelt Érettségi", getTasksForCategory("erettsegi_emelt", "Emelt Szintű Érettségi", folders, files, zipBytes));
        result.put("Emelt Informatika Ismeretek", getTasksForCategory("informatika_ismeretek/emelt", "Emelt Szintű Informatika Ismeretek", folders, files, zipBytes));
        result.put("Közép Informatika Ismeretek", getTasksForCategory("informatika_ismeretek/kozep", "Közép Szintű Informatika Ismeretek", folders, files, zipBytes));
        result.put("OKJ Rendszerüzemeltető", getTasksForCategory("okj/rendszeruzemelteto", "OKJ Rendszerüzemeltető", folders, files, zipBytes));
        result.put("OKJ Szoftverfejlesztő", getTasksForCategory("okj/szoftverfejleszto", "OKJ Szoftverfejlesztő", folders, files, zipBytes));

        return result;
    }

    private static List<Task> getTasksForCategory(String categoryPrefix, String typeName, List<String> folders, List<String> files, byte[] zipBytes) {
        var expectedSlashCount = categoryPrefix.chars().filter(k -> k == '/').count() + 2;
        var nameSubstringBegin = categoryPrefix.length() + 1;

        return folders.stream()
                      .filter(k -> k.startsWith(categoryPrefix) && k.chars().filter(l -> l == '/').count() == expectedSlashCount)
                      .map(k -> k.substring(nameSubstringBegin, k.length() - 1))
                      .map(k -> new Task(k, categoryPrefix, typeName, files, zipBytes))
                      .peek(k -> downloadTaskResourceFiles(k, zipBytes))
                      .sorted(Comparator.<Task>comparingInt(k -> k.year).reversed())
                      .collect(Collectors.toList());
    }

    private static void downloadTaskResourceFiles(Task task, byte[] zipBytes) {
        try {
            var taskResourcesCacheFolder = Files.createDirectories(Path.of(CACHE_FOLDER + "/" + task.ID + "/resources"));
            var taskSolutionsCacheFolder = Files.createDirectory(Path.of(CACHE_FOLDER + "/" + task.ID + "/solutions"));

            Arrays.stream(task.resourceFileEntryNames)
                  .forEach(k -> extractResourceFile(taskResourcesCacheFolder, k, zipBytes));

            Arrays.stream(task.solutions)
                  .map(k -> k.optionalFileToDownload)
                  .filter(k -> k.startsWith("https"))
                  .forEach(k -> extractResourceFile(taskSolutionsCacheFolder, k, zipBytes));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void extractResourceFile(Path folder, String entry, byte[] zipBytes) {
        var entryData = IOUtils.getZipEntry(zipBytes, entry);

        try {
            Files.write(Path.of(folder + "/" + entry.substring(entry.lastIndexOf('/') + 1)), entryData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] readAllBytesFromFile(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}