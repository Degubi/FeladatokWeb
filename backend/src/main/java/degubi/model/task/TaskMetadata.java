package degubi.model.task;

import java.util.*;

public record TaskMetadata(String[] consoleInput, Map<String, String[]> solutions) {}