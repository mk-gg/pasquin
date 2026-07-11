package dev.mkgg.notes.image;

/** Port for storing uploaded note images. */
public interface ImageStore {

  /**
   * Stores the image and returns the public URL it will be served from.
   *
   * @param key object key, e.g. {@code images/{userId}/{uuid}.png}
   * @param bytes the validated image bytes
   * @param contentType the detected content type, e.g. {@code image/png}
   */
  String store(String key, byte[] bytes, String contentType);
}
