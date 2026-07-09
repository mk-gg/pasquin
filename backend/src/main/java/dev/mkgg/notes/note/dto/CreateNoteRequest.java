package dev.mkgg.notes.note.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

/**
 * Payload sent by the editor's save dialog.
 *
 * @param content the Tiptap document as JSON
 * @param title display title, or {@code null} to fall back to "Untitled note"
 * @param autoExpire expiry label such as {@code "7 Days"}, or {@code null} to keep forever
 * @param password plaintext password to protect the note with, or {@code null}
 */
public record CreateNoteRequest(
    @NotNull JsonNode content,
    @Size(max = 200) String title,
    String autoExpire,
    @Size(min = 4, max = 72) String password) {}
