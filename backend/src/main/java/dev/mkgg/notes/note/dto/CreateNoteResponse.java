package dev.mkgg.notes.note.dto;

import java.time.Instant;

/**
 * Result of saving a note. The edit key is returned exactly once, here; the server keeps only a
 * hash and cannot recover it.
 *
 * @param slug identifier to build the shareable link from
 * @param title display title of the note
 * @param expiresAt when the note will be deleted, or {@code null} if it never expires
 * @param passwordProtected whether a password is required to read the note
 * @param editKey secret that authorizes future updates and deletion of this note
 */
public record CreateNoteResponse(
    String slug, String title, Instant expiresAt, boolean passwordProtected, String editKey) {}
