package dev.mkgg.notes.billing;

import dev.mkgg.notes.config.NotesProperties;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives Polar webhook deliveries. The signature is verified against the endpoint secret before
 * any payload is parsed; unverified requests get a 403 and verified ones always get a 202 so Polar
 * does not retry events we deliberately ignore.
 */
@RestController
@RequestMapping("/api/webhooks/polar")
public class PolarWebhookController {

  private final BillingService billingService;
  private final NotesProperties.Polar polar;
  private final Clock clock;

  public PolarWebhookController(
      BillingService billingService, NotesProperties properties, Clock clock) {
    this.billingService = billingService;
    this.polar = properties.polar();
    this.clock = clock;
  }

  @PostMapping
  public ResponseEntity<Void> receive(
      @RequestHeader(value = "webhook-id", required = false) String id,
      @RequestHeader(value = "webhook-timestamp", required = false) String timestamp,
      @RequestHeader(value = "webhook-signature", required = false) String signature,
      @RequestBody String payload) {
    if (!polar.enabled()
        || !WebhookSignatureVerifier.verify(
            polar.webhookSecret(), id, timestamp, signature, payload, clock)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    billingService.handleWebhookEvent(payload);
    return ResponseEntity.accepted().build();
  }
}
