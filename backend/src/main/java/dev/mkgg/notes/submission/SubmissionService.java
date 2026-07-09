package dev.mkgg.notes.submission;

import dev.mkgg.notes.submission.dto.ContactRequest;
import dev.mkgg.notes.submission.dto.ReportRequest;
import dev.mkgg.notes.submission.storage.SubmissionRepository;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Persists contact messages and abuse reports. */
@Service
public class SubmissionService {

  private final SubmissionRepository repository;
  private final Clock clock;

  public SubmissionService(SubmissionRepository repository, Clock clock) {
    this.repository = repository;
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
    repository.save(
        new Submission(newId(), SubmissionType.CONTACT, clock.instant(), request.email(), data));
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
    repository.save(
        new Submission(newId(), SubmissionType.REPORT, clock.instant(), request.email(), data));
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
