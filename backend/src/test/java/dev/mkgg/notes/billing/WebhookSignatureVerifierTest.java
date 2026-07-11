package dev.mkgg.notes.billing;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class WebhookSignatureVerifierTest {

  private static final String SECRET = "whsec_test_secret";
  private static final String ID = "msg_123";
  private static final String PAYLOAD = "{\"type\":\"order.paid\"}";
  private static final Instant NOW = Instant.parse("2026-07-11T10:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  private static String sign(String secret, String id, long timestamp, String payload)
      throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    byte[] digest =
        mac.doFinal((id + "." + timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
    return "v1," + Base64.getEncoder().encodeToString(digest);
  }

  @Test
  void acceptsValidSignature() throws Exception {
    long ts = NOW.getEpochSecond();
    String header = sign(SECRET, ID, ts, PAYLOAD);

    assertThat(
            WebhookSignatureVerifier.verify(SECRET, ID, String.valueOf(ts), header, PAYLOAD, CLOCK))
        .isTrue();
  }

  @Test
  void acceptsValidSignatureAmongMultipleEntries() throws Exception {
    long ts = NOW.getEpochSecond();
    String header = "v1,Zm9yZ2VkIQ== " + sign(SECRET, ID, ts, PAYLOAD);

    assertThat(
            WebhookSignatureVerifier.verify(SECRET, ID, String.valueOf(ts), header, PAYLOAD, CLOCK))
        .isTrue();
  }

  @Test
  void rejectsTamperedPayload() throws Exception {
    long ts = NOW.getEpochSecond();
    String header = sign(SECRET, ID, ts, PAYLOAD);

    assertThat(
            WebhookSignatureVerifier.verify(
                SECRET, ID, String.valueOf(ts), header, "{\"type\":\"order.refunded\"}", CLOCK))
        .isFalse();
  }

  @Test
  void rejectsWrongSecret() throws Exception {
    long ts = NOW.getEpochSecond();
    String header = sign("other_secret", ID, ts, PAYLOAD);

    assertThat(
            WebhookSignatureVerifier.verify(SECRET, ID, String.valueOf(ts), header, PAYLOAD, CLOCK))
        .isFalse();
  }

  @Test
  void rejectsStaleTimestamp() throws Exception {
    long stale = NOW.minusSeconds(600).getEpochSecond();
    String header = sign(SECRET, ID, stale, PAYLOAD);

    assertThat(
            WebhookSignatureVerifier.verify(
                SECRET, ID, String.valueOf(stale), header, PAYLOAD, CLOCK))
        .isFalse();
  }

  @Test
  void rejectsMissingHeadersOrBlankSecret() {
    assertThat(WebhookSignatureVerifier.verify(SECRET, null, "1", "v1,x", PAYLOAD, CLOCK))
        .isFalse();
    assertThat(WebhookSignatureVerifier.verify(SECRET, ID, null, "v1,x", PAYLOAD, CLOCK)).isFalse();
    assertThat(WebhookSignatureVerifier.verify(SECRET, ID, "1", null, PAYLOAD, CLOCK)).isFalse();
    assertThat(WebhookSignatureVerifier.verify("", ID, "1", "v1,x", PAYLOAD, CLOCK)).isFalse();
    assertThat(WebhookSignatureVerifier.verify(SECRET, ID, "not-a-number", "v1,x", PAYLOAD, CLOCK))
        .isFalse();
  }
}
