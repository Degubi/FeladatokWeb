package degubi.model.user;

import java.util.*;
import jakarta.persistence.*;

@Entity
@Table(name = "UserTaskStatuses")
public final class UserTaskStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public final UUID id;
    public final String taskId;
    public final int completedSubtasks;
    @ManyToOne(fetch = FetchType.LAZY)
    public final User user;

    protected UserTaskStatus() {
        this.id = null;
        this.taskId = null;
        this.completedSubtasks = 0;
        this.user = null;
    }

    public UserTaskStatus(UUID id, String taskId, int completedSubtasks, User user) {
        this.id = id;
        this.taskId = taskId;
        this.completedSubtasks = completedSubtasks;
        this.user = user;
    }
}