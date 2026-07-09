package dev.mkgg.notes.submission.storage;

import dev.mkgg.notes.config.NotesProperties;
import dev.mkgg.notes.submission.Submission;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/** DynamoDB-backed submission store. */
@Repository
@Profile("aws")
public class DynamoDbSubmissionRepository implements SubmissionRepository {

  private final DynamoDbTable<SubmissionItem> table;

  public DynamoDbSubmissionRepository(DynamoDbEnhancedClient client, NotesProperties properties) {
    this.table =
        client.table(
            properties.aws().submissionsTable(), TableSchema.fromBean(SubmissionItem.class));
  }

  @Override
  public void save(Submission submission) {
    table.putItem(SubmissionItem.fromDomain(submission));
  }
}
