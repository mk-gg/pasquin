package dev.mkgg.notes.note;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Generates short, URL-safe, unguessable note identifiers. */
@Component
public class SlugGenerator {

  private static final String ALPHABET =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private static final int LENGTH = 10;

  private final SecureRandom random = new SecureRandom();

  public String generate() {
    StringBuilder slug = new StringBuilder(LENGTH);
    for (int i = 0; i < LENGTH; i++) {
      slug.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
    }
    return slug.toString();
  }
}
