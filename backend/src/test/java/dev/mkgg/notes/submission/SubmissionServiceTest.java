package dev.mkgg.notes.submission;

import static org.assertj.core.api.Assertions.assertThat;

import dev.mkgg.notes.submission.dto.ContactRequest;
import dev.mkgg.notes.submission.dto.ReportRequest;
import dev.mkgg.notes.submission.storage.InMemorySubmissionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubmissionServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-07T10:00:00Z");

  private InMemorySubmissionRepository repository;
  private SubmissionService service;

  @BeforeEach
  void setUp() {
    repository = new InMemorySubmissionRepository();
    service = new SubmissionService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
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
  void honeypotSubmissionsAreDroppedSilently() {
    service.submitContact(
        new ContactRequest("Bot", "bot@example.com", "General inquiry", "spam", "http://spam"));
    service.submitReport(
        new ReportRequest("Spam", "bot@example.com", "spam", null, null, "filled"));

    assertThat(repository.findAll()).isEmpty();
  }
}
