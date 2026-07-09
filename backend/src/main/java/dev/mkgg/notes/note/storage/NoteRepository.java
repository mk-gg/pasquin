package dev.mkgg.notes.note.storage;

import dev.mkgg.notes.note.NoteMetadata;
import java.util.Optional;

/** Port for note metadata persistence. */
public interface NoteRepository {

  void save(NoteMetadata metadata);

  Optional<NoteMetadata> findBySlug(String slug);

  void deleteBySlug(String slug);
}
