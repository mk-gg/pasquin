package dev.mkgg.notes.auth.storage;

import dev.mkgg.notes.auth.User;
import dev.mkgg.notes.config.NotesProperties;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/** DynamoDB-backed user store. */
@Repository
@Profile("aws")
public class DynamoDbUserRepository implements UserRepository {

  private final DynamoDbTable<UserItem> table;

  public DynamoDbUserRepository(DynamoDbEnhancedClient client, NotesProperties properties) {
    this.table = client.table(properties.aws().usersTable(), TableSchema.fromBean(UserItem.class));
  }

  @Override
  public Optional<User> findById(String id) {
    UserItem item = table.getItem(Key.builder().partitionValue(id).build());
    return Optional.ofNullable(item).map(UserItem::toDomain);
  }

  @Override
  public void save(User user) {
    table.putItem(UserItem.fromDomain(user));
  }
}
