package dev.mkgg.notes.note.storage;

import java.util.Optional;

/** Port for note document body storage; content is the Tiptap document serialized as JSON. */
public interface NoteContentStore {

  void put(String slug, String contentJson);

  Optional<String> get(String slug);

  void delete(String slug);
}
