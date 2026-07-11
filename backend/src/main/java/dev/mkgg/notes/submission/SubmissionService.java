package dev.mkgg.notes.submission;

import dev.mkgg.notes.submission.dto.ContactRequest;
import dev.mkgg.notes.submission.dto.ReportRequest;
import dev.mkgg.notes.submission.notify.SubmissionNotifier;
import dev.mkgg.notes.submission.storage.SubmissionRepository;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Persists contact messages and abuse reports, then notifies the site owner. */
@Service
public class SubmissionService {

  private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);

  private final SubmissionRepository repository;
  private final SubmissionNotifier notifier;
  private final Clock clock;

  public SubmissionService(
      SubmissionRepository repository, SubmissionNotifier notifier, Clock clock) {
    this.repository = repository;
    this.notifier = notifier;
    this.clock = clock;
  }

  public void submitContact(ContactRequest request) {
    if (isBot(request.website())) {
      return;
    }
    Map<String, String> data = new LinkedHashMap<>();
    data.put("name", request.name());
    data.put("reason", request.reason());
    data.put("message", request.message());
    store(new Submission(newId(), SubmissionType.CONTACT, clock.instant(), request.email(), data));
  }

  public void submitReport(ReportRequest request) {
    if (isBot(request.website())) {
      return;
    }
    Map<String, String> data = new LinkedHashMap<>();
    data.put("type", request.type());
    putIfPresent(data, "details", request.details());
    putIfPresent(data, "links", request.links());
    putIfPresent(data, "noteSlug", request.noteSlug());
    store(new Submission(newId(), SubmissionType.REPORT, clock.instant(), request.email(), data));
  }

  /**
   * Saves, then notifies. The submission is already stored, so a notification failure is logged
   * rather than surfaced — the submitter's form must not error over an email hiccup.
   */
  private void store(Submission submission) {
    repository.save(submission);
    try {
      notifier.notifyNew(submission);
    } catch (RuntimeException e) {
      log.error("Failed to send notification for submission {}", submission.id(), e);
    }
  }

  /** A filled honeypot field means a bot; silently accept without storing. */
  private static boolean isBot(String honeypot) {
    return honeypot != null && !honeypot.isBlank();
  }

  private static void putIfPresent(Map<String, String> data, String key, String value) {
    if (value != null && !value.isBlank()) {
      data.put(key, value);
    }
  }

  private static String newId() {
    return UUID.randomUUID().toString();
  }
}
