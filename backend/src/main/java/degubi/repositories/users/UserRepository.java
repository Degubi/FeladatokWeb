package degubi.repositories.users;

import org.springframework.data.jpa.repository.*;
import degubi.model.user.*;

public interface UserRepository extends JpaRepository<User, Long> {}