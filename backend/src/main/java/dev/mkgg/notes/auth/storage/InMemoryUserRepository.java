package dev.mkgg.notes.auth.storage;

import dev.mkgg.notes.auth.User;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/** Map-backed user store for local development without AWS credentials. */
@Repository
@Profile("!aws")
public class InMemoryUserRepository implements UserRepository {

  private final Map<String, User> users = new ConcurrentHashMap<>();

  @Override
  public Optional<User> findById(String id) {
    return Optional.ofNullable(users.get(id));
  }

  @Override
  public void save(User user) {
    users.put(user.id(), user);
  }
}
