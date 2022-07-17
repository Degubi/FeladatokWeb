package degubi.model;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

public class TaskMetadata {

    public final String[] consoleInput;
    public final Map<String, String[]> solutions;

    @JsonCreator
    public TaskMetadata(@JsonProperty("consoleInput") String[] consoleInput,
                        @JsonProperty("solutions") Map<String, String[]> solutions) {

        this.consoleInput = consoleInput;
        this.solutions = solutions;
    }
}