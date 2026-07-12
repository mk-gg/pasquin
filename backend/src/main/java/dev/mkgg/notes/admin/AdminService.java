package dev.mkgg.notes.admin;

import dev.mkgg.notes.image.ImageStore;
import dev.mkgg.notes.note.storage.NoteContentStore;
import dev.mkgg.notes.note.storage.NoteRepository;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Moderation takedowns. Unlike a normal delete, no edit key is needed and the note's embedded
 * images are removed too — including evicting them from the CDN edge caches, since image objects
 * are cached for a year.
 */
@Service
public class AdminService {

  private static final Logger log = LoggerFactory.getLogger(AdminService.class);

  /** Image URLs embedded in note content: /images/{userId}/{uuid}.{ext} */
  private static final Pattern IMAGE_KEY =
      Pattern.compile("/(images/[A-Za-z0-9_-]+/[A-Za-z0-9-]+\\.(?:png|jpe?g|webp|gif))");

  private final NoteRepository noteRepository;
  private final NoteContentStore contentStore;
  private final ImageStore imageStore;
  private final CdnInvalidator cdnInvalidator;

  public AdminService(
      NoteRepository noteRepository,
      NoteContentStore contentStore,
      ImageStore imageStore,
      CdnInvalidator cdnInvalidator) {
    this.noteRepository = noteRepository;
    this.contentStore = contentStore;
    this.imageStore = imageStore;
    this.cdnInvalidator = cdnInvalidator;
  }

  /**
   * Takes a note down: metadata, content body, and any embedded images. Best-effort and idempotent
   * — a partially removed note can be taken down again.
   */
  public void takeDownNote(String slug) {
    List<String> imageKeys =
        contentStore.get(slug).map(AdminService::extractImageKeys).orElse(List.of());
    noteRepository.deleteBySlug(slug);
    contentStore.delete(slug);
    for (String key : imageKeys) {
      imageStore.delete(key);
    }
    cdnInvalidator.invalidate(imageKeys.stream().map(key -> "/" + key).toList());
    log.info("Took down note {} ({} embedded image(s))", slug, imageKeys.size());
  }

  private static List<String> extractImageKeys(String contentJson) {
    Matcher matcher = IMAGE_KEY.matcher(contentJson);
    return matcher.results().map(result -> result.group(1)).distinct().toList();
  }
}
