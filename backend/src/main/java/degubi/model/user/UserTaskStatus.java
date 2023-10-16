package degubi.model.user;

import jakarta.persistence.*;

@Entity
@Table(name = "UserTaskStatuses")
public record UserTaskStatus(
    @Id
    @GeneratedValue
    long id,
    String taskId,
    @ManyToOne(fetch = FetchType.LAZY)
    User user
) {}