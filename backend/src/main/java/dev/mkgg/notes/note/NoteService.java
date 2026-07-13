package dev.mkgg.notes.note;

import dev.mkgg.notes.common.InvalidEditKeyException;
import dev.mkgg.notes.common.InvalidPasswordException;
import dev.mkgg.notes.common.ResourceNotFoundException;
import dev.mkgg.notes.note.dto.CreateNoteRequest;
import dev.mkgg.notes.note.dto.CreateNoteResponse;
import dev.mkgg.notes.note.dto.NoteResponse;
import dev.mkgg.notes.note.dto.UpdateNoteRequest;
import dev.mkgg.notes.note.storage.NoteContentStore;
import dev.mkgg.notes.note.storage.NoteRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Application logic for creating and reading notes. */
@Service
public class NoteService {

  private final NoteRepository noteRepository;
  private final NoteContentStore contentStore;
  private final SlugGenerator slugGenerator;
  private final PasswordEncoder passwordEncoder;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public NoteService(
      NoteRepository noteRepository,
      NoteContentStore contentStore,
      SlugGenerator slugGenerator,
      PasswordEncoder passwordEncoder,
      ObjectMapper objectMapper,
      Clock clock) {
    this.noteRepository = noteRepository;
    this.contentStore = contentStore;
    this.slugGenerator = slugGenerator;
    this.passwordEncoder = passwordEncoder;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /** Stores a new note and returns its shareable identifier and one-time edit key. */
  public CreateNoteResponse create(CreateNoteRequest request) {
    Instant now = clock.instant();
    Instant expiresAt =
        request.autoExpire() == null
            ? null
            : now.plus(ExpiryOption.fromLabel(request.autoExpire()).duration());
    String passwordHash =
        request.password() == null ? null : passwordEncoder.encode(request.password());

    String slug = slugGenerator.generate();
    String title = normalizeTitle(request.title());
    String editKey = EditKeys.generate();
    NoteMetadata metadata =
        new NoteMetadata(slug, title, now, now, expiresAt, passwordHash, EditKeys.hash(editKey));

    contentStore.put(slug, request.content().toString());
    noteRepository.save(metadata);

    return new CreateNoteResponse(slug, title, expiresAt, metadata.isPasswordProtected(), editKey);
  }

  /**
   * Applies a partial update (content, title, password and/or expiry); requires the note's edit
   * key.
   */
  public NoteResponse update(String slug, String editKey, UpdateNoteRequest request) {
    boolean removeExpiry = Boolean.TRUE.equals(request.removeExpiry());
    if (request.content() == null
        && request.title() == null
        && request.password() == null
        && request.autoExpire() == null
        && !removeExpiry) {
      throw new IllegalArgumentException("Nothing to update");
    }
    NoteMetadata metadata = findActive(slug);
    requireEditKey(metadata, editKey);

    if (request.content() != null) {
      contentStore.put(slug, request.content().toString());
    }
    if (request.title() != null) {
      metadata = metadata.withTitle(normalizeTitle(request.title()));
    }
    if (request.password() != null) {
      metadata = metadata.withPasswordHash(passwordEncoder.encode(request.password()));
    }
    Instant now = clock.instant();
    if (removeExpiry) {
      metadata = metadata.withExpiresAt(null);
    } else if (request.autoExpire() != null) {
      metadata =
          metadata.withExpiresAt(now.plus(ExpiryOption.fromLabel(request.autoExpire()).duration()));
    }
    metadata = metadata.withUpdatedAt(now);
    noteRepository.save(metadata);

    JsonNode content = request.content() != null ? request.content() : readContent(slug);
    return toResponse(metadata, content);
  }

  /**
   * Deletes a note; requires the note's edit key. Only the metadata is removed here — that alone
   * makes the note unreachable (every read resolves metadata first). The content body and any
   * embedded images are swept asynchronously by the cleanup Lambda listening to the table's stream,
   * the same path that cleans up TTL-expired notes.
   */
  public void delete(String slug, String editKey) {
    NoteMetadata metadata = findActive(slug);
    requireEditKey(metadata, editKey);
    noteRepository.deleteBySlug(slug);
  }

  /**
   * Returns a note by slug. Password-protected notes are returned without content unless the caller
   * presents the note's edit key (the owner); readers unlock them via {@link #unlock}.
   */
  public NoteResponse get(String slug, String editKey) {
    NoteMetadata metadata = findActive(slug);
    boolean owner = editKey != null && EditKeys.matches(editKey, metadata.editKeyHash());
    if (metadata.isPasswordProtected() && !owner) {
      // The title is derived from the note's first line, so it must stay
      // hidden along with the content until the reader unlocks the note.
      return lockedResponse(metadata);
    }
    return toResponse(metadata, readContent(slug));
  }

  /** Verifies the password of a protected note and returns it with content. */
  public NoteResponse unlock(String slug, String password) {
    NoteMetadata metadata = findActive(slug);
    if (!metadata.isPasswordProtected()) {
      return toResponse(metadata, readContent(slug));
    }
    if (!passwordEncoder.matches(password, metadata.passwordHash())) {
      throw new InvalidPasswordException();
    }
    return toResponse(metadata, readContent(slug));
  }

  private NoteMetadata findActive(String slug) {
    NoteMetadata metadata =
        noteRepository
            .findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Note not found: " + slug));
    if (metadata.isExpired(clock.instant())) {
      // DynamoDB TTL deletes lazily; treat expired-but-present items as gone.
      throw new ResourceNotFoundException("Note not found: " + slug);
    }
    return metadata;
  }

  private JsonNode readContent(String slug) {
    String json =
        contentStore
            .get(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Note not found: " + slug));
    try {
      return objectMapper.readTree(json);
    } catch (JacksonException e) {
      throw new IllegalStateException("Stored note content is not valid JSON: " + slug, e);
    }
  }

  private static void requireEditKey(NoteMetadata metadata, String editKey) {
    if (editKey == null || !EditKeys.matches(editKey, metadata.editKeyHash())) {
      throw new InvalidEditKeyException();
    }
  }

  private static String normalizeTitle(String title) {
    if (title == null || title.isBlank()) {
      return "Untitled note";
    }
    return title.strip();
  }

  private static NoteResponse toResponse(NoteMetadata metadata, JsonNode content) {
    return new NoteResponse(
        metadata.slug(),
        metadata.title(),
        content,
        metadata.isPasswordProtected(),
        metadata.createdAt(),
        metadata.updatedAt(),
        metadata.expiresAt());
  }

  /**
   * Response for a protected note viewed without the password: reveals nothing but the slug and
   * that a password is required. Title, content, timestamps and expiry all stay hidden until
   * unlocked.
   */
  private static NoteResponse lockedResponse(NoteMetadata metadata) {
    return new NoteResponse(metadata.slug(), null, null, true, null, null, null);
  }
}
