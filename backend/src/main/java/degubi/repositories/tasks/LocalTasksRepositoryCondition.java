package degubi.repositories.tasks;

import java.nio.file.*;
import org.springframework.context.annotation.*;
import org.springframework.core.type.*;

public final class LocalTasksRepositoryCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return Files.exists(TasksRepository.LOCAL_TASKS_PATH);
    }
}