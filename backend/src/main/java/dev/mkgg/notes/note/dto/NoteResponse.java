package dev.mkgg.notes.note.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

/**
 * A note as returned to readers. For password-protected notes fetched without unlocking, {@code
 * content} is {@code null} and the client should prompt for the password.
 *
 * @param slug note identifier
 * @param title display title of the note
 * @param content the Tiptap document, or {@code null} when locked
 * @param passwordProtected whether the note requires a password
 * @param createdAt creation time
 * @param updatedAt time of the last change
 * @param expiresAt expiry time, or {@code null} if the note never expires
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NoteResponse(
    String slug,
    String title,
    JsonNode content,
    boolean passwordProtected,
    Instant createdAt,
    Instant updatedAt,
    Instant expiresAt) {}
