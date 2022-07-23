package degubi.model;

import degubi.utils.*;
import java.util.*;
import java.util.stream.*;

public final class Task {
    private static final ParsedSolution[] EMPTY_SOLUTION_ARRAY = new ParsedSolution[0];
    private static final String[] SOLUTION_FILE_EXTENSIONS = { ".java", ".cs", ".py", ".rs", ".fsx" };
    private static final String[] RESOURCE_FILE_EXTENSIONS = { ".txt", ".csv" };
    private static int taskOrdinal = 1;

    public final String ID;
    public final String name;
    public final int year;
    public final String month;
    public final int subtaskCount;
    public final String pdfPath;
    public final String[] resourceFileEntryNames;
    public final Map<String, List<String>> solutionFilePathsPerExtension;

    public final transient String[] consoleInput;
    public final transient ParsedSolution[] solutions;

    public Task(String taskFolderName, Map<String, List<String>> filesPerExtension, byte[] zipBytes) {
        var nameSplit = taskFolderName.split("_", 2);
        var pdfFilePath = filesPerExtension.get(".pdf").get(0);
        var optionalYear = parseOptionalInt(nameSplit[0]);

        this.name = pdfFilePath.substring(pdfFilePath.lastIndexOf('/') + 1, pdfFilePath.lastIndexOf('.'));
        this.ID = this.name + "_" + taskOrdinal++;
        this.year = optionalYear;
        this.month = optionalYear == -1 ? "" : nameSplit[1];
        this.pdfPath = pdfFilePath;
        this.resourceFileEntryNames = Arrays.stream(RESOURCE_FILE_EXTENSIONS)
                                            .map(k -> filesPerExtension.getOrDefault(k, List.of()))
                                            .flatMap(List::stream)
                                            .toArray(String[]::new);

        this.solutionFilePathsPerExtension = Arrays.stream(SOLUTION_FILE_EXTENSIONS)
                                                   .map(k -> filesPerExtension.getOrDefault(k, List.of()))
                                                   .flatMap(List::stream)
                                                   .collect(Collectors.groupingBy(k -> k.substring(k.lastIndexOf('.') + 1)));

        var optionalWebJson = filesPerExtension.get(".json");
        if(optionalWebJson != null) {
            var jsonBytes = IOUtils.getZipEntry(zipBytes, optionalWebJson.get(0));
            var taskMetadata = IOUtils.parseJsonObject(jsonBytes, TaskMetadata.class);

            this.consoleInput = taskMetadata.consoleInput;
            this.solutions = taskMetadata.solutions.entrySet().stream()
                                         .map(e -> new ParsedSolution(e.getValue(), Integer.parseInt(e.getKey()), null))
                                         .toArray(ParsedSolution[]::new);
        }else{
            this.consoleInput = null;
            this.solutions = EMPTY_SOLUTION_ARRAY;
        }

        this.subtaskCount = this.solutions.length;
    }

    private static int parseOptionalInt(String year) {
        try {
            return Integer.parseInt(year);
        }catch(NumberFormatException e) {
            return -1;
        }
    }
}