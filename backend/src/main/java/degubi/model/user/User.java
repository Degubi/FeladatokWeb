package degubi.model.user;

import java.util.*;
import jakarta.persistence.*;

@Entity
@Table(name = "\"User\"")
public final class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public final UUID id;
    @OneToMany(mappedBy = "user")
    public final UserTaskStatus[] taskStatuses;

    protected User() {
        this.id = null;
        this.taskStatuses = null;
    }

    public User(UUID id, UserTaskStatus[] taskStatuses) {
        this.id = id;
        this.taskStatuses = taskStatuses;
    }
}