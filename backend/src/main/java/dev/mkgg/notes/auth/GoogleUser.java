package dev.mkgg.notes.auth;

/**
 * Identity extracted from a verified Google ID token.
 *
 * @param sub Google's stable user identifier (used as our user id)
 * @param email the user's email address
 * @param name the user's display name, or {@code null}
 * @param picture URL of the user's profile picture, or {@code null}. Not persisted: Google
 *     rotates these URLs, so the frontend refreshes it on every sign-in.
 */
public record GoogleUser(String sub, String email, String name, String picture) {}
