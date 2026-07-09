package dev.mkgg.notes.auth;

import java.util.List;

/**
 * A signed-in user account.
 *
 * @param id Google's stable subject identifier
 * @param email the user's email
 * @param name display name, or {@code null}
 * @param notes the notes this user owns
 */
public record User(String id, String email, String name, List<OwnedNote> notes) {

  public User withProfile(String newEmail, String newName) {
    return new User(id, newEmail, newName, notes);
  }

  public User withNotes(List<OwnedNote> newNotes) {
    return new User(id, email, name, newNotes);
  }
}
