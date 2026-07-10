package dev.mkgg.notes.auth.dto;

/**
 * Result of a successful sign-in.
 *
 * @param token our session token, sent as a Bearer token on subsequent requests
 * @param email the user's email
 * @param name the user's display name, or {@code null}
 * @param picture URL of the user's Google profile picture, or {@code null}
 */
public record AuthResponse(String token, String email, String name, String picture) {}
