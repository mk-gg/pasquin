package dev.mkgg.notes.submission.notify;

import dev.mkgg.notes.submission.Submission;

/** Alerts the site owner that a new submission was stored. */
public interface SubmissionNotifier {

  void notifyNew(Submission submission);
}
