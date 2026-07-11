package dev.mkgg.notes.auth;

import jakarta.validation.constraints.Size;

/**
 * A note owned by a signed-in user, mirroring the browser's local record so the two can sync.
 * Timestamps are ISO-8601 strings, as the frontend stores them. Field sizes are bounded so a
 * signed-in client cannot bloat its account item toward the DynamoDB item-size limit.
 *
 * @param slug note identifier
 * @param editKey the note's edit key (lets the user edit it from any device)
 * @param title display title (matches the note title limit)
 * @param createdAt creation time (ISO-8601)
 * @param expiresAt expiry time (ISO-8601), or {@code null}
 */
public record OwnedNote(
    @Size(max = 64) String slug,
    @Size(max = 128) String editKey,
    @Size(max = 200) String title,
    @Size(max = 40) String createdAt,
    @Size(max = 40) String expiresAt) {}
