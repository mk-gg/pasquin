package dev.mkgg.notes.submission.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contact form submission.
 *
 * @param name submitter's name
 * @param email submitter's email address
 * @param reason reason for contact (e.g. "General inquiry")
 * @param message the message body
 * @param website honeypot: must stay empty; real users never see this field
 */
public record ContactRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(max = 50) String reason,
    @NotBlank @Size(max = 5000) String message,
    String website) {}
