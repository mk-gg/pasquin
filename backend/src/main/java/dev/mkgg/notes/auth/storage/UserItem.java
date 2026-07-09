package dev.mkgg.notes.auth.storage;

import dev.mkgg.notes.auth.User;
import java.util.List;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/** DynamoDB item mapping for a user account and its embedded note list. */
@DynamoDbBean
public class UserItem {

  private String id;
  private String email;
  private String name;
  private List<OwnedNoteItem> notes;

  public static UserItem fromDomain(User user) {
    UserItem item = new UserItem();
    item.setId(user.id());
    item.setEmail(user.email());
    item.setName(user.name());
    item.setNotes(user.notes().stream().map(OwnedNoteItem::fromDomain).toList());
    return item;
  }

  public User toDomain() {
    List<OwnedNoteItem> source = notes == null ? List.of() : notes;
    return new User(id, email, name, source.stream().map(OwnedNoteItem::toDomain).toList());
  }

  @DynamoDbPartitionKey
  @DynamoDbAttribute("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @DynamoDbAttribute("email")
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @DynamoDbAttribute("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @DynamoDbAttribute("notes")
  public List<OwnedNoteItem> getNotes() {
    return notes;
  }

  public void setNotes(List<OwnedNoteItem> notes) {
    this.notes = notes;
  }
}
