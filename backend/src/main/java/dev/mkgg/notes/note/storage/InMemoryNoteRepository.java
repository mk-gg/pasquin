package dev.mkgg.notes.note.storage;

import dev.mkgg.notes.note.NoteMetadata;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/** Map-backed metadata store for local development without AWS credentials. */
@Repository
@Profile("!aws")
public class InMemoryNoteRepository implements NoteRepository {

  private final Map<String, NoteMetadata> notes = new ConcurrentHashMap<>();

  @Override
  public void save(NoteMetadata metadata) {
    notes.put(metadata.slug(), metadata);
  }

  @Override
  public Optional<NoteMetadata> findBySlug(String slug) {
    return Optional.ofNullable(notes.get(slug));
  }

  @Override
  public void deleteBySlug(String slug) {
    notes.remove(slug);
  }
}
