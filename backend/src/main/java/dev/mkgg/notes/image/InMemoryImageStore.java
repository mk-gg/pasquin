package dev.mkgg.notes.image;

import java.util.Base64;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Local-development stand-in: returns the image as a data URL so nothing needs to be served. The
 * bytes are embedded straight into the note document.
 */
@Component
@Profile("!aws")
public class InMemoryImageStore implements ImageStore {

  @Override
  public String store(String key, byte[] bytes, String contentType) {
    return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
  }
}
