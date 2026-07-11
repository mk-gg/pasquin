package dev.mkgg.notes.billing;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verifies <a href="https://www.standardwebhooks.com">Standard Webhooks</a> signatures, as sent by
 * Polar: {@code HMAC-SHA256(secret, "{id}.{timestamp}.{payload}")}, base64-encoded, delivered in a
 * space-separated {@code webhook-signature} header of {@code v1,<base64>} entries.
 *
 * <p>The configured secret's UTF-8 bytes are used as the HMAC key verbatim — paste the secret from
 * the Polar dashboard as-is.
 */
public final class WebhookSignatureVerifier {

  /** Rejects replayed deliveries; generous enough for clock skew and retry backoff. */
  private static final Duration TIMESTAMP_TOLERANCE = Duration.ofMinutes(5);

  private WebhookSignatureVerifier() {}

  /**
   * Returns whether the delivery is authentic and fresh.
   *
   * @param secret the endpoint's signing secret
   * @param id the {@code webhook-id} header
   * @param timestamp the {@code webhook-timestamp} header (Unix seconds)
   * @param signatureHeader the {@code webhook-signature} header
   * @param payload the raw request body
   * @param clock time source for the replay check
   */
  public static boolean verify(
      String secret,
      String id,
      String timestamp,
      String signatureHeader,
      String payload,
      Clock clock) {
    if (secret == null
        || secret.isBlank()
        || id == null
        || timestamp == null
        || signatureHeader == null
        || payload == null) {
      return false;
    }
    Instant sentAt;
    try {
      sentAt = Instant.ofEpochSecond(Long.parseLong(timestamp.trim()));
    } catch (NumberFormatException e) {
      return false;
    }
    Duration age = Duration.between(sentAt, clock.instant()).abs();
    if (age.compareTo(TIMESTAMP_TOLERANCE) > 0) {
      return false;
    }
    byte[] expected = hmac(secret, id + "." + timestamp.trim() + "." + payload);
    for (String entry : signatureHeader.split(" ")) {
      String[] parts = entry.split(",", 2);
      if (parts.length == 2 && "v1".equals(parts[0]) && matches(expected, parts[1])) {
        return true;
      }
    }
    return false;
  }

  private static boolean matches(byte[] expected, String providedBase64) {
    byte[] provided;
    try {
      provided = Base64.getDecoder().decode(providedBase64);
    } catch (IllegalArgumentException e) {
      return false;
    }
    return MessageDigest.isEqual(expected, provided);
  }

  private static byte[] hmac(String secret, String content) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("HMAC-SHA256 unavailable", e);
    }
  }
}
