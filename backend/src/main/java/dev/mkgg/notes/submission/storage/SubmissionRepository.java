package dev.mkgg.notes.submission.storage;

import dev.mkgg.notes.submission.Submission;

/** Port for persisting contact/report submissions. */
public interface SubmissionRepository {

  void save(Submission submission);
}
