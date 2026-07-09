package dev.mkgg.notes.auth;

/**
 * Identity extracted from a verified Google ID token.
 *
 * @param sub Google's stable user identifier (used as our user id)
 * @param email the user's email address
 * @param name the user's display name, or {@code null}
 */
public record GoogleUser(String sub, String email, String name) {}
