package dev.mkgg.notes.submission.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Abuse report submission, from either the standalone report page or the in-note report dialog.
 *
 * @param type report category (e.g. "Spam")
 * @param email reporter's email address
 * @param details free-text description of the violation
 * @param links link(s) to the offending note(s)
 * @param noteSlug slug of the reported note when reported from the viewer
 * @param website honeypot: must stay empty; real users never see this field
 */
public record ReportRequest(
    @NotBlank @Size(max = 50) String type,
    @NotBlank @Email @Size(max = 254) String email,
    @Size(max = 5000) String details,
    @Size(max = 2000) String links,
    @Size(max = 64) String noteSlug,
    String website) {}
