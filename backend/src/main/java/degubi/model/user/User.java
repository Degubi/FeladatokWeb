package degubi.model.user;

import jakarta.persistence.*;

@Entity
@Table(name = "\"User\"")
public record User(
    @Id
    @GeneratedValue
    long id,
    String name,
    @OneToMany(mappedBy = "user")
    UserTaskStatus[] taskStatuses
) {}