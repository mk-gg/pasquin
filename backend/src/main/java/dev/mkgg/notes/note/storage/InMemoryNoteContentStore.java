package dev.mkgg.notes.note.storage;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Map-backed content store for local development without AWS credentials. */
@Component
@Profile("!aws")
public class InMemoryNoteContentStore implements NoteContentStore {

  private final Map<String, String> contents = new ConcurrentHashMap<>();

  @Override
  public void put(String slug, String contentJson) {
    contents.put(slug, contentJson);
  }

  @Override
  public Optional<String> get(String slug) {
    return Optional.ofNullable(contents.get(slug));
  }

  @Override
  public void delete(String slug) {
    contents.remove(slug);
  }
}
