package dev.mkgg.notes.image;

import dev.mkgg.notes.auth.User;
import dev.mkgg.notes.auth.storage.UserRepository;
import dev.mkgg.notes.common.InvalidTokenException;
import dev.mkgg.notes.config.NotesProperties;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Premium image uploads. The content type is detected from the file's magic bytes — the client's
 * declared type is never trusted — and each upload counts against a per-user lifetime quota so
 * storage cost stays bounded.
 */
@Service
public class ImageService {

  private static final Logger log = LoggerFactory.getLogger(ImageService.class);

  private final ImageStore imageStore;
  private final UserRepository userRepository;
  private final NotesProperties.Images limits;

  public ImageService(
      ImageStore imageStore, UserRepository userRepository, NotesProperties properties) {
    this.imageStore = imageStore;
    this.userRepository = userRepository;
    this.limits = properties.images();
  }

  /** Validates and stores an image for the given user; returns its public URL. */
  public String upload(String userId, byte[] bytes) {
    User user = userRepository.findById(userId).orElseThrow(InvalidTokenException::new);
    if (!user.premium()) {
      throw new PremiumRequiredException();
    }
    if (bytes == null || bytes.length == 0) {
      throw new IllegalArgumentException("Empty image");
    }
    if (bytes.length > limits.maxImageBytes()) {
      throw new IllegalArgumentException(
          "Image exceeds the maximum size of " + limits.maxImageBytes() + " bytes");
    }
    ImageType type = ImageType.detect(bytes);
    if (type == null) {
      throw new IllegalArgumentException("Unsupported image format (png, jpeg, webp, gif only)");
    }
    if (user.imageBytes() + bytes.length > limits.maxTotalBytesPerUser()) {
      throw new QuotaExceededException();
    }
    String key = "images/" + user.id() + "/" + UUID.randomUUID() + "." + type.extension;
    String url = imageStore.store(key, bytes, type.contentType);
    userRepository.save(user.withImageBytes(user.imageBytes() + bytes.length));
    log.info("Stored {}-byte {} for user {}", bytes.length, type.contentType, user.id());
    return url;
  }

  /** Supported formats, identified by their leading magic bytes. */
  enum ImageType {
    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpg"),
    GIF("image/gif", "gif"),
    WEBP("image/webp", "webp");

    final String contentType;
    final String extension;

    ImageType(String contentType, String extension) {
      this.contentType = contentType;
      this.extension = extension;
    }

    static ImageType detect(byte[] bytes) {
      if (startsWith(bytes, 0x89, 0x50, 0x4E, 0x47)) {
        return PNG;
      }
      if (startsWith(bytes, 0xFF, 0xD8, 0xFF)) {
        return JPEG;
      }
      if (startsWith(bytes, 'G', 'I', 'F', '8')) {
        return GIF;
      }
      // RIFF....WEBP
      if (startsWith(bytes, 'R', 'I', 'F', 'F')
          && bytes.length >= 12
          && bytes[8] == 'W'
          && bytes[9] == 'E'
          && bytes[10] == 'B'
          && bytes[11] == 'P') {
        return WEBP;
      }
      return null;
    }

    private static boolean startsWith(byte[] bytes, int... prefix) {
      if (bytes.length < prefix.length) {
        return false;
      }
      for (int i = 0; i < prefix.length; i++) {
        if ((bytes[i] & 0xFF) != prefix[i]) {
          return false;
        }
      }
      return true;
    }
  }
}
