package dev.mkgg.notes.note.storage;

import dev.mkgg.notes.config.NotesProperties;
import dev.mkgg.notes.note.NoteMetadata;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/** DynamoDB-backed metadata store; expired items are removed by the table's TTL setting. */
@Repository
@Profile("aws")
public class DynamoDbNoteRepository implements NoteRepository {

  private final DynamoDbTable<NoteItem> table;

  public DynamoDbNoteRepository(DynamoDbEnhancedClient client, NotesProperties properties) {
    this.table = client.table(properties.aws().tableName(), TableSchema.fromBean(NoteItem.class));
  }

  @Override
  public void save(NoteMetadata metadata) {
    table.putItem(NoteItem.fromMetadata(metadata));
  }

  @Override
  public Optional<NoteMetadata> findBySlug(String slug) {
    NoteItem item = table.getItem(Key.builder().partitionValue(slug).build());
    return Optional.ofNullable(item).map(NoteItem::toMetadata);
  }

  @Override
  public void deleteBySlug(String slug) {
    table.deleteItem(Key.builder().partitionValue(slug).build());
  }
}
