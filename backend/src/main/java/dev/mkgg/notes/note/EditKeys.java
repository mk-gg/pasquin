package dev.mkgg.notes.note;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Edit keys prove note ownership without accounts: generated once at creation, shown to the creator
 * once, and stored server-side only as a SHA-256 hash. The key has 128 bits of entropy, so a fast
 * unsalted hash is appropriate (unlike user-chosen passwords).
 */
public final class EditKeys {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int KEY_BYTES = 16;

  private EditKeys() {}

  /** Returns a new random edit key as 32 lowercase hex characters. */
  public static String generate() {
    byte[] bytes = new byte[KEY_BYTES];
    RANDOM.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  /** Returns the SHA-256 hash of a key as hex, as stored in note metadata. */
  public static String hash(String editKey) {
    return HexFormat.of().formatHex(sha256().digest(editKey.getBytes(StandardCharsets.UTF_8)));
  }

  /** Constant-time check of a presented key against the stored hash. */
  public static boolean matches(String editKey, String expectedHash) {
    byte[] actual = hash(editKey).getBytes(StandardCharsets.UTF_8);
    byte[] expected = expectedHash.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(actual, expected);
  }

  private static MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
