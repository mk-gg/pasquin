package dev.mkgg.notes.auth;

/**
 * A note owned by a signed-in user, mirroring the browser's local record so the two can sync.
 * Timestamps are ISO-8601 strings, as the frontend stores them.
 *
 * @param slug note identifier
 * @param editKey the note's edit key (lets the user edit it from any device)
 * @param title display title
 * @param createdAt creation time (ISO-8601)
 * @param expiresAt expiry time (ISO-8601), or {@code null}
 */
public record OwnedNote(
    String slug, String editKey, String title, String createdAt, String expiresAt) {}
