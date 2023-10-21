package degubi.model.task;

import java.util.*;

public record TaskListResponse(
   String ID,
   String name,
   int year,
   String month,
   int totalSubtaskCount,
   int completedSubtaskCount,
   String pdfPath,
   String[] resourceFileEntryNames,
   Map<String, List<String>> solutionFilePathsPerExtension
) {}