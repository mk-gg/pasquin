package dev.mkgg.notes.auth;

import java.util.List;

/**
 * A signed-in user account.
 *
 * @param id Google's stable subject identifier
 * @param email the user's email
 * @param name display name, or {@code null}
 * @param premium whether the user has purchased premium (set via the Polar webhook)
 * @param notes the notes this user owns
 */
public record User(String id, String email, String name, boolean premium, List<OwnedNote> notes) {

  public User withProfile(String newEmail, String newName) {
    return new User(id, newEmail, newName, premium, notes);
  }

  public User withNotes(List<OwnedNote> newNotes) {
    return new User(id, email, name, premium, newNotes);
  }

  public User withPremium(boolean newPremium) {
    return new User(id, email, name, newPremium, notes);
  }
}
