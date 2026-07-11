package dev.mkgg.notes.submission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import dev.mkgg.notes.submission.dto.ContactRequest;
import dev.mkgg.notes.submission.dto.ReportRequest;
import dev.mkgg.notes.submission.notify.SubmissionNotifier;
import dev.mkgg.notes.submission.storage.InMemorySubmissionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubmissionServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-07T10:00:00Z");

  private InMemorySubmissionRepository repository;
  private RecordingNotifier notifier;
  private SubmissionService service;

  @BeforeEach
  void setUp() {
    repository = new InMemorySubmissionRepository();
    notifier = new RecordingNotifier();
    service = new SubmissionService(repository, notifier, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void storesContactSubmission() {
    service.submitContact(
        new ContactRequest("Ada", "ada@example.com", "General inquiry", "Hello there", null));

    assertThat(repository.findAll()).hasSize(1);
    Submission stored = repository.findAll().get(0);
    assertThat(stored.type()).isEqualTo(SubmissionType.CONTACT);
    assertThat(stored.email()).isEqualTo("ada@example.com");
    assertThat(stored.createdAt()).isEqualTo(NOW);
    assertThat(stored.data())
        .containsEntry("name", "Ada")
        .containsEntry("reason", "General inquiry")
        .containsEntry("message", "Hello there");
    assertThat(stored.id()).isNotBlank();
  }

  @Test
  void storesReportSubmissionWithOnlyPresentFields() {
    service.submitReport(
        new ReportRequest("Spam", "reporter@example.com", "spammy links", null, "abc123", null));

    Submission stored = repository.findAll().get(0);
    assertThat(stored.type()).isEqualTo(SubmissionType.REPORT);
    assertThat(stored.data())
        .containsEntry("type", "Spam")
        .containsEntry("details", "spammy links")
        .containsEntry("noteSlug", "abc123")
        .doesNotContainKey("links");
  }

  @Test
  void notifiesForEachStoredSubmission() {
    service.submitContact(
        new ContactRequest("Ada", "ada@example.com", "General inquiry", "Hello there", null));
    service.submitReport(
        new ReportRequest("Spam", "reporter@example.com", "spammy links", null, "abc123", null));

    assertThat(notifier.notified).hasSize(2);
    assertThat(notifier.notified.get(0).type()).isEqualTo(SubmissionType.CONTACT);
    assertThat(notifier.notified.get(1).type()).isEqualTo(SubmissionType.REPORT);
  }

  @Test
  void honeypotSubmissionsAreDroppedSilently() {
    service.submitContact(
        new ContactRequest("Bot", "bot@example.com", "General inquiry", "spam", "http://spam"));
    service.submitReport(
        new ReportRequest("Spam", "bot@example.com", "spam", null, null, "filled"));

    assertThat(repository.findAll()).isEmpty();
    assertThat(notifier.notified).isEmpty();
  }

  @Test
  void notificationFailureDoesNotFailTheSubmission() {
    notifier.failNext = true;

    assertThatCode(
            () ->
                service.submitContact(
                    new ContactRequest("Ada", "ada@example.com", "General inquiry", "Hi", null)))
        .doesNotThrowAnyException();
    assertThat(repository.findAll()).hasSize(1);
  }

  private static final class RecordingNotifier implements SubmissionNotifier {
    final List<Submission> notified = new ArrayList<>();
    boolean failNext;

    @Override
    public void notifyNew(Submission submission) {
      if (failNext) {
        failNext = false;
        throw new IllegalStateException("SES unavailable");
      }
      notified.add(submission);
    }
  }
}
