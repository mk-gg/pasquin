package dev.mkgg.notes.note.dto;

import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

/**
 * Partial update of a note; at least one field must be present. Content-only for autosave,
 * title-only for rename, password-only to (re)protect, and expiry changes via {@code autoExpire} /
 * {@code removeExpiry}.
 *
 * @param content replacement Tiptap document, or {@code null} to leave unchanged
 * @param title replacement display title, or {@code null} to leave unchanged
 * @param password new password for the note, or {@code null} to leave unchanged
 * @param autoExpire expiry label such as {@code "7 Days"} to (re)set the expiry, or {@code null}
 * @param removeExpiry when {@code true}, clears the expiry so the note never expires
 */
public record UpdateNoteRequest(
    JsonNode content,
    @Size(max = 200) String title,
    @Size(min = 4, max = 72) String password,
    String autoExpire,
    Boolean removeExpiry) {}
