package dev.mkgg.notes.admin;

import static org.assertj.core.api.Assertions.assertThat;

import dev.mkgg.notes.image.ImageStore;
import dev.mkgg.notes.note.NoteMetadata;
import dev.mkgg.notes.note.storage.InMemoryNoteContentStore;
import dev.mkgg.notes.note.storage.InMemoryNoteRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminServiceTest {

  private InMemoryNoteRepository notes;
  private InMemoryNoteContentStore contents;
  private List<String> deletedImageKeys;
  private List<String> invalidatedPaths;
  private AdminService service;

  @BeforeEach
  void setUp() {
    notes = new InMemoryNoteRepository();
    contents = new InMemoryNoteContentStore();
    deletedImageKeys = new ArrayList<>();
    invalidatedPaths = new ArrayList<>();
    ImageStore imageStore =
        new ImageStore() {
          @Override
          public String store(String key, byte[] bytes, String contentType) {
            return key;
          }

          @Override
          public void delete(String key) {
            deletedImageKeys.add(key);
          }
        };
    service = new AdminService(notes, contents, imageStore, invalidatedPaths::addAll);
  }

  private void putNote(String slug, String contentJson) {
    Instant now = Instant.parse("2026-07-12T00:00:00Z");
    notes.save(new NoteMetadata(slug, "t", now, now, null, null, "hash"));
    contents.put(slug, contentJson);
  }

  @Test
  void removesMetadataAndContent() {
    putNote("abc123", "{\"type\":\"doc\"}");

    service.takeDownNote("abc123");

    assertThat(notes.findBySlug("abc123")).isEmpty();
    assertThat(contents.get("abc123")).isEmpty();
    assertThat(deletedImageKeys).isEmpty();
    assertThat(invalidatedPaths).isEmpty();
  }

  @Test
  void removesEmbeddedImagesAndInvalidatesCdn() {
    putNote(
        "abc123",
        "{\"content\":[{\"type\":\"image\",\"attrs\":{\"src\":"
            + "\"https://pasquin.mkgg.dev/images/user-1/11111111-aaaa-4bbb-8ccc-2222.png\"}},"
            + "{\"type\":\"image\",\"attrs\":{\"src\":"
            + "\"https://pasquin.mkgg.dev/images/user-1/33333333-dddd-4eee-9fff-4444.webp\"}}]}");

    service.takeDownNote("abc123");

    assertThat(deletedImageKeys)
        .containsExactlyInAnyOrder(
            "images/user-1/11111111-aaaa-4bbb-8ccc-2222.png",
            "images/user-1/33333333-dddd-4eee-9fff-4444.webp");
    assertThat(invalidatedPaths)
        .containsExactlyInAnyOrder(
            "/images/user-1/11111111-aaaa-4bbb-8ccc-2222.png",
            "/images/user-1/33333333-dddd-4eee-9fff-4444.webp");
  }

  @Test
  void takedownIsIdempotentWhenNoteIsAlreadyGone() {
    service.takeDownNote("missing");

    assertThat(notes.findBySlug("missing")).isEmpty();
    assertThat(deletedImageKeys).isEmpty();
    assertThat(invalidatedPaths).isEmpty();
  }
}
