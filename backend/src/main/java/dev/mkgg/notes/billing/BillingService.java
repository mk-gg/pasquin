package dev.mkgg.notes.billing;

import dev.mkgg.notes.auth.storage.UserRepository;
import dev.mkgg.notes.common.InvalidTokenException;
import dev.mkgg.notes.config.NotesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Premium purchases: creates Polar checkouts and applies paid orders from webhooks. */
@Service
public class BillingService {

  private static final Logger log = LoggerFactory.getLogger(BillingService.class);

  private final PolarClient polarClient;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;
  private final NotesProperties.Polar polar;

  public BillingService(
      PolarClient polarClient,
      UserRepository userRepository,
      ObjectMapper objectMapper,
      NotesProperties properties) {
    this.polarClient = polarClient;
    this.userRepository = userRepository;
    this.objectMapper = objectMapper;
    this.polar = properties.polar();
  }

  /** Creates a checkout session for the signed-in user and returns its hosted URL. */
  public String createCheckout(String userId) {
    if (!polar.enabled()) {
      throw new BillingUnavailableException("Billing is not enabled");
    }
    var user = userRepository.findById(userId).orElseThrow(InvalidTokenException::new);
    return polarClient.createCheckoutUrl(user.id(), user.email());
  }

  /**
   * Applies a verified webhook event. Only {@code order.paid} changes state: the order's {@code
   * customer.external_id} is our user id, and that user becomes premium. Idempotent — replays and
   * unknown event types are ignored.
   */
  public void handleWebhookEvent(String payload) {
    JsonNode event;
    try {
      event = objectMapper.readTree(payload);
    } catch (JacksonException e) {
      log.warn("Ignoring unparseable Polar webhook payload");
      return;
    }
    String type = event.path("type").asText("");
    if (!"order.paid".equals(type)) {
      log.debug("Ignoring Polar webhook event type {}", type);
      return;
    }
    String userId = event.path("data").path("customer").path("external_id").asText("");
    if (userId.isBlank()) {
      log.warn("Polar order.paid event had no customer external_id; cannot apply");
      return;
    }
    userRepository
        .findById(userId)
        .ifPresentOrElse(
            user -> {
              userRepository.save(user.withPremium(true));
              log.info("Premium activated for user {}", userId);
            },
            () -> log.warn("Polar order.paid for unknown user {}", userId));
  }
}
