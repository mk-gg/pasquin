package dev.mkgg.notes.auth.storage;

import dev.mkgg.notes.auth.OwnedNote;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/** DynamoDB nested-document mapping for one owned note within a user item. */
@DynamoDbBean
public class OwnedNoteItem {

  private String slug;
  private String editKey;
  private String title;
  private String createdAt;
  private String expiresAt;

  public static OwnedNoteItem fromDomain(OwnedNote note) {
    OwnedNoteItem item = new OwnedNoteItem();
    item.setSlug(note.slug());
    item.setEditKey(note.editKey());
    item.setTitle(note.title());
    item.setCreatedAt(note.createdAt());
    item.setExpiresAt(note.expiresAt());
    return item;
  }

  public OwnedNote toDomain() {
    return new OwnedNote(slug, editKey, title, createdAt, expiresAt);
  }

  @DynamoDbAttribute("slug")
  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  @DynamoDbAttribute("editKey")
  public String getEditKey() {
    return editKey;
  }

  public void setEditKey(String editKey) {
    this.editKey = editKey;
  }

  @DynamoDbAttribute("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @DynamoDbAttribute("createdAt")
  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  @DynamoDbAttribute("expiresAt")
  public String getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(String expiresAt) {
    this.expiresAt = expiresAt;
  }
}
