package dev.mkgg.notes.note;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.mkgg.notes.common.InvalidEditKeyException;
import dev.mkgg.notes.common.InvalidPasswordException;
import dev.mkgg.notes.common.ResourceNotFoundException;
import dev.mkgg.notes.note.dto.CreateNoteRequest;
import dev.mkgg.notes.note.dto.CreateNoteResponse;
import dev.mkgg.notes.note.dto.NoteResponse;
import dev.mkgg.notes.note.dto.UpdateNoteRequest;
import dev.mkgg.notes.note.storage.InMemoryNoteContentStore;
import dev.mkgg.notes.note.storage.InMemoryNoteRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import tools.jackson.databind.ObjectMapper;

class NoteServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-06T12:00:00Z");

  private final ObjectMapper objectMapper = new ObjectMapper();
  private MutableClock clock;
  private NoteService noteService;

  @BeforeEach
  void setUp() {
    clock = new MutableClock(NOW);
    noteService =
        new NoteService(
            new InMemoryNoteRepository(),
            new InMemoryNoteContentStore(),
            new SlugGenerator(),
            new BCryptPasswordEncoder(),
            objectMapper,
            clock);
  }

  private CreateNoteRequest request(String autoExpire, String password) {
    return new CreateNoteRequest(
        objectMapper.createObjectNode().put("type", "doc"), "My note", autoExpire, password);
  }

  @Test
  void createReturnsSlugAndRoundTripsContent() {
    CreateNoteResponse created = noteService.create(request(null, null));

    assertThat(created.slug()).hasSize(10);
    assertThat(created.title()).isEqualTo("My note");
    assertThat(created.expiresAt()).isNull();
    assertThat(created.passwordProtected()).isFalse();

    NoteResponse fetched = noteService.get(created.slug(), null);
    assertThat(fetched.content().get("type").asText()).isEqualTo("doc");
    assertThat(fetched.title()).isEqualTo("My note");
  }

  @Test
  void createReturnsAnEditKeyAndBlankTitleGetsDefault() {
    CreateNoteResponse created =
        noteService.create(
            new CreateNoteRequest(
                objectMapper.createObjectNode().put("type", "doc"), "  ", null, null));

    assertThat(created.editKey()).hasSize(32).matches("[0-9a-f]+");
    assertThat(created.title()).isEqualTo("Untitled note");
  }

  @Test
  void updateWithCorrectKeyReplacesContent() {
    CreateNoteResponse created = noteService.create(request(null, null));

    NoteResponse updated =
        noteService.update(
            created.slug(),
            created.editKey(),
            new UpdateNoteRequest(
                objectMapper.createObjectNode().put("type", "doc2"), null, null, null, null));

    assertThat(updated.content().get("type").asText()).isEqualTo("doc2");
    assertThat(noteService.get(created.slug(), null).content().get("type").asText())
        .isEqualTo("doc2");
  }

  @Test
  void renameChangesTitleWithoutTouchingContent() {
    CreateNoteResponse created = noteService.create(request(null, null));

    NoteResponse renamed =
        noteService.update(
            created.slug(),
            created.editKey(),
            new UpdateNoteRequest(null, "Renamed", null, null, null));

    assertThat(renamed.title()).isEqualTo("Renamed");
    assertThat(renamed.content().get("type").asText()).isEqualTo("doc");
  }

  @Test
  void updateWithWrongOrMissingKeyIsRejected() {
    CreateNoteResponse created = noteService.create(request(null, null));
    UpdateNoteRequest update = new UpdateNoteRequest(null, "Hijacked", null, null, null);

    assertThatThrownBy(() -> noteService.update(created.slug(), "0".repeat(32), update))
        .isInstanceOf(InvalidEditKeyException.class);
    assertThatThrownBy(() -> noteService.update(created.slug(), null, update))
        .isInstanceOf(InvalidEditKeyException.class);
  }

  @Test
  void emptyUpdateIsRejected() {
    CreateNoteResponse created = noteService.create(request(null, null));

    assertThatThrownBy(
            () ->
                noteService.update(
                    created.slug(),
                    created.editKey(),
                    new UpdateNoteRequest(null, null, null, null, null)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void deleteWithCorrectKeyRemovesNote() {
    CreateNoteResponse created = noteService.create(request(null, null));

    noteService.delete(created.slug(), created.editKey());

    assertThatThrownBy(() -> noteService.get(created.slug(), null))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void deleteWithWrongKeyIsRejected() {
    CreateNoteResponse created = noteService.create(request(null, null));

    assertThatThrownBy(() -> noteService.delete(created.slug(), "0".repeat(32)))
        .isInstanceOf(InvalidEditKeyException.class);
    assertThat(noteService.get(created.slug(), null)).isNotNull();
  }

  @Test
  void autoExpireLabelSetsExpiry() {
    CreateNoteResponse created = noteService.create(request("7 Days", null));

    assertThat(created.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
  }

  @Test
  void unknownExpiryLabelIsRejected() {
    assertThatThrownBy(() -> noteService.create(request("2 Fortnights", null)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void expiredNoteBehavesAsMissing() {
    CreateNoteResponse created = noteService.create(request("1 Hour", null));

    clock.advance(Duration.ofHours(2));

    assertThatThrownBy(() -> noteService.get(created.slug(), null))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void protectedNoteHidesContentUntilUnlocked() {
    CreateNoteResponse created = noteService.create(request(null, "hunter22"));

    NoteResponse locked = noteService.get(created.slug(), null);
    assertThat(locked.passwordProtected()).isTrue();
    assertThat(locked.content()).isNull();
    assertThat(locked.title()).isNull();

    NoteResponse unlocked = noteService.unlock(created.slug(), "hunter22");
    assertThat(unlocked.content()).isNotNull();
    assertThat(unlocked.title()).isEqualTo("My note");
  }

  @Test
  void lockedNoteHidesExpiryUntilUnlocked() {
    CreateNoteResponse created = noteService.create(request("7 Days", "hunter22"));

    NoteResponse locked = noteService.get(created.slug(), null);
    assertThat(locked.expiresAt()).isNull();
    assertThat(locked.createdAt()).isNull();

    NoteResponse unlocked = noteService.unlock(created.slug(), "hunter22");
    assertThat(unlocked.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
  }

  @Test
  void passwordCanBeSetAfterCreation() {
    CreateNoteResponse created = noteService.create(request(null, null));

    NoteResponse updated =
        noteService.update(
            created.slug(),
            created.editKey(),
            new UpdateNoteRequest(null, null, "newpass1", null, null));
    assertThat(updated.passwordProtected()).isTrue();

    assertThat(noteService.get(created.slug(), null).content()).isNull();
    assertThat(noteService.unlock(created.slug(), "newpass1").content()).isNotNull();
  }

  @Test
  void expiryCanBeSetChangedAndRemovedViaUpdate() {
    CreateNoteResponse created = noteService.create(request(null, null));
    assertThat(noteService.get(created.slug(), null).expiresAt()).isNull();

    NoteResponse set =
        noteService.update(
            created.slug(),
            created.editKey(),
            new UpdateNoteRequest(null, null, null, "7 Days", null));
    assertThat(set.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));

    NoteResponse changed =
        noteService.update(
            created.slug(),
            created.editKey(),
            new UpdateNoteRequest(null, null, null, "1 Hour", null));
    assertThat(changed.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));

    NoteResponse removed =
        noteService.update(
            created.slug(), created.editKey(), new UpdateNoteRequest(null, null, null, null, true));
    assertThat(removed.expiresAt()).isNull();
  }

  @Test
  void updateBumpsUpdatedAtButNotCreatedAt() {
    CreateNoteResponse created = noteService.create(request(null, null));
    clock.advance(Duration.ofMinutes(5));

    NoteResponse updated =
        noteService.update(
            created.slug(),
            created.editKey(),
            new UpdateNoteRequest(null, "Renamed", null, null, null));

    assertThat(updated.createdAt()).isEqualTo(NOW);
    assertThat(updated.updatedAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
  }

  @Test
  void ownerReadsProtectedNoteWithEditKey() {
    CreateNoteResponse created = noteService.create(request(null, "hunter22"));

    NoteResponse asOwner = noteService.get(created.slug(), created.editKey());
    assertThat(asOwner.content()).isNotNull();

    NoteResponse withWrongKey = noteService.get(created.slug(), "0".repeat(32));
    assertThat(withWrongKey.content()).isNull();
  }

  @Test
  void wrongPasswordIsRejected() {
    CreateNoteResponse created = noteService.create(request(null, "hunter22"));

    assertThatThrownBy(() -> noteService.unlock(created.slug(), "wrong"))
        .isInstanceOf(InvalidPasswordException.class);
  }

  @Test
  void missingSlugIsNotFound() {
    assertThatThrownBy(() -> noteService.get("nope", null))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  /** Fixed clock that tests can advance to simulate the passage of time. */
  private static final class MutableClock extends Clock {

    private Instant instant;

    private MutableClock(Instant instant) {
      this.instant = instant;
    }

    private void advance(Duration duration) {
      instant = instant.plus(duration);
    }

    @Override
    public Instant instant() {
      return instant;
    }

    @Override
    public java.time.ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }
  }
}
