package dev.mkgg.notes.submission;

import java.time.Instant;
import java.util.Map;

/**
 * A stored contact message or abuse report. Type-specific fields live in {@code data} so the two
 * submission kinds share one storage shape.
 *
 * @param id unique identifier
 * @param type contact or report
 * @param createdAt submission time
 * @param email submitter's email address
 * @param data type-specific fields (e.g. name/reason/message, or reportType/details/noteSlug)
 */
public record Submission(
    String id, SubmissionType type, Instant createdAt, String email, Map<String, String> data) {}
