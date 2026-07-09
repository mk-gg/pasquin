package dev.mkgg.notes.note.storage;

import dev.mkgg.notes.note.NoteMetadata;
import java.time.Instant;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * DynamoDB item mapping for note metadata. {@code expiresAt} is stored as epoch seconds so the
 * table's TTL setting deletes expired notes automatically.
 */
@DynamoDbBean
public class NoteItem {

  private String slug;
  private String title;
  private long createdAt;
  private long updatedAt;
  private Long expiresAt;
  private String passwordHash;
  private String editKeyHash;

  public static NoteItem fromMetadata(NoteMetadata metadata) {
    NoteItem item = new NoteItem();
    item.setSlug(metadata.slug());
    item.setTitle(metadata.title());
    item.setCreatedAt(metadata.createdAt().getEpochSecond());
    item.setUpdatedAt(metadata.updatedAt().getEpochSecond());
    item.setExpiresAt(metadata.expiresAt() == null ? null : metadata.expiresAt().getEpochSecond());
    item.setPasswordHash(metadata.passwordHash());
    item.setEditKeyHash(metadata.editKeyHash());
    return item;
  }

  public NoteMetadata toMetadata() {
    return new NoteMetadata(
        slug,
        title,
        Instant.ofEpochSecond(createdAt),
        Instant.ofEpochSecond(updatedAt),
        expiresAt == null ? null : Instant.ofEpochSecond(expiresAt),
        passwordHash,
        editKeyHash);
  }

  @DynamoDbPartitionKey
  @DynamoDbAttribute("slug")
  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  @DynamoDbAttribute("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @DynamoDbAttribute("createdAt")
  public long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }

  @DynamoDbAttribute("updatedAt")
  public long getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
  }

  @DynamoDbAttribute("expiresAt")
  public Long getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Long expiresAt) {
    this.expiresAt = expiresAt;
  }

  @DynamoDbAttribute("passwordHash")
  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  @DynamoDbAttribute("editKeyHash")
  public String getEditKeyHash() {
    return editKeyHash;
  }

  public void setEditKeyHash(String editKeyHash) {
    this.editKeyHash = editKeyHash;
  }
}
