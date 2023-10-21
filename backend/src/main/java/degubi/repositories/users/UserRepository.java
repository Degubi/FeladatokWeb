package degubi.repositories.users;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import degubi.model.user.*;

public interface UserRepository extends JpaRepository<User, Long> {
    User findById(UUID id);
}