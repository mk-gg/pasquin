package dev.mkgg.notes.note.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Password submission for reading a protected note.
 *
 * @param password the plaintext password to verify
 */
public record UnlockNoteRequest(@NotBlank String password) {}
