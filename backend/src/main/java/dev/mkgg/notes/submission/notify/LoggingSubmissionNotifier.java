package dev.mkgg.notes.submission.notify;

import dev.mkgg.notes.submission.Submission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Log-only notifier for local development without AWS credentials. */
@Component
@Profile("!aws")
public class LoggingSubmissionNotifier implements SubmissionNotifier {

  private static final Logger log = LoggerFactory.getLogger(LoggingSubmissionNotifier.class);

  @Override
  public void notifyNew(Submission submission) {
    log.info(
        "New {} submission {} from {}", submission.type(), submission.id(), submission.email());
  }
}
