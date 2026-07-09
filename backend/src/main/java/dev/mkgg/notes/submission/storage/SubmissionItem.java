package dev.mkgg.notes.submission.storage;

import dev.mkgg.notes.submission.Submission;
import dev.mkgg.notes.submission.SubmissionType;
import java.time.Instant;
import java.util.Map;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/** DynamoDB item mapping for a submission. */
@DynamoDbBean
public class SubmissionItem {

  private String id;
  private String type;
  private long createdAt;
  private String email;
  private Map<String, String> data;

  public static SubmissionItem fromDomain(Submission submission) {
    SubmissionItem item = new SubmissionItem();
    item.setId(submission.id());
    item.setType(submission.type().name());
    item.setCreatedAt(submission.createdAt().getEpochSecond());
    item.setEmail(submission.email());
    item.setData(submission.data());
    return item;
  }

  public Submission toDomain() {
    return new Submission(
        id, SubmissionType.valueOf(type), Instant.ofEpochSecond(createdAt), email, data);
  }

  @DynamoDbPartitionKey
  @DynamoDbAttribute("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @DynamoDbAttribute("type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @DynamoDbAttribute("createdAt")
  public long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }

  @DynamoDbAttribute("email")
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @DynamoDbAttribute("data")
  public Map<String, String> getData() {
    return data;
  }

  public void setData(Map<String, String> data) {
    this.data = data;
  }
}
