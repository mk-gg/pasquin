package dev.mkgg.notes.note;

import java.time.Instant;

/**
 * Metadata for a stored note; the document body itself lives in the content store.
 *
 * @param slug URL-safe identifier of the note
 * @param title display title, shown in the viewer and the owner's note list
 * @param createdAt creation time
 * @param updatedAt time of the last content/title/password change
 * @param expiresAt expiry time, or {@code null} if the note never expires
 * @param passwordHash BCrypt hash of the password, or {@code null} if not protected
 * @param editKeyHash SHA-256 hash of the edit key that authorizes updates and deletion
 */
public record NoteMetadata(
    String slug,
    String title,
    Instant createdAt,
    Instant updatedAt,
    Instant expiresAt,
    String passwordHash,
    String editKeyHash) {

  public boolean isPasswordProtected() {
    return passwordHash != null;
  }

  public boolean isExpired(Instant now) {
    return expiresAt != null && now.isAfter(expiresAt);
  }

  public NoteMetadata withTitle(String newTitle) {
    return new NoteMetadata(
        slug, newTitle, createdAt, updatedAt, expiresAt, passwordHash, editKeyHash);
  }

  public NoteMetadata withPasswordHash(String newPasswordHash) {
    return new NoteMetadata(
        slug, title, createdAt, updatedAt, expiresAt, newPasswordHash, editKeyHash);
  }

  public NoteMetadata withUpdatedAt(Instant newUpdatedAt) {
    return new NoteMetadata(
        slug, title, createdAt, newUpdatedAt, expiresAt, passwordHash, editKeyHash);
  }

  public NoteMetadata withExpiresAt(Instant newExpiresAt) {
    return new NoteMetadata(
        slug, title, createdAt, updatedAt, newExpiresAt, passwordHash, editKeyHash);
  }
}
