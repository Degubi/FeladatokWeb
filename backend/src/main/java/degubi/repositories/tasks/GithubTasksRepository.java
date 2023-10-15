package degubi.repositories.tasks;

import java.util.*;
import org.springframework.boot.web.client.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.*;
import degubi.model.*;

@Repository
@Conditional(GithubTasksRepositoryCondition.class)
public final class GithubTasksRepository implements TasksRepository {

    private final LinkedHashMap<String, Task[]> perCategory;
    private final Map<String, Task> perID;

    public GithubTasksRepository(RestTemplateBuilder templateBuilder) {
        System.out.println("Initializing task cache from Github");

        var perCategoryMap = TasksRepository.createPerCategoryMap(templateBuilder.build().getForObject("https://github.com/Degubi/Feladatok/archive/master.zip", byte[].class));
        perCategory = perCategoryMap;
        perID = TasksRepository.createPerIDMap(perCategoryMap);

        System.out.println("Task cache initialization done");
    }

    @Override public LinkedHashMap<String, Task[]> perCategoryTasks() { return perCategory; }
    @Override public Map<String, Task> perIDTasks() { return perID; }
}