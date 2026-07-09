package dev.mkgg.notes.auth.storage;

import dev.mkgg.notes.auth.User;
import java.util.Optional;

/** Port for user-account persistence. */
public interface UserRepository {

  Optional<User> findById(String id);

  void save(User user);
}
