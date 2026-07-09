package dev.mkgg.notes.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Sign-in payload from the frontend.
 *
 * @param idToken the Google ID token returned by Google Identity Services
 */
public record GoogleSignInRequest(@NotBlank String idToken) {}
