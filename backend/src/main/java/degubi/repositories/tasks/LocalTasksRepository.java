package degubi.repositories.tasks;

import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.*;
import degubi.model.*;
import degubi.utils.*;

@Repository
@Conditional(LocalTasksRepositoryCondition.class)
public final class LocalTasksRepository implements TasksRepository {

    private final LinkedHashMap<String, Task[]> perCategory;
    private final Map<String, Task> perID;

    public LocalTasksRepository() {
        System.out.println("Initializing task cache from local zip");

        var perCategoryMap = TasksRepository.createPerCategoryMap(IOUtils.readAllBytesFromFile(TasksRepository.LOCAL_TASKS_PATH));
        perCategory = perCategoryMap;
        perID = TasksRepository.createPerIDMap(perCategoryMap);

        System.out.println("Task cache initialization done");
    }

    @Override public LinkedHashMap<String, Task[]> perCategoryTasks() { return perCategory; }
    @Override public Map<String, Task> perIDTasks() { return perID; }
}