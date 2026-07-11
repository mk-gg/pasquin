package dev.mkgg.notes.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.mkgg.notes.auth.User;
import dev.mkgg.notes.auth.storage.InMemoryUserRepository;
import dev.mkgg.notes.config.NotesProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class BillingServiceTest {

  private InMemoryUserRepository users;
  private BillingService service;
  private String checkoutRequestedFor;

  private NotesProperties props(boolean enabled) {
    return new NotesProperties(
        new NotesProperties.Cors(List.of()),
        new NotesProperties.Aws("r", "t", "b", "s", "u"),
        new NotesProperties.RateLimit(1, 1, 1, 1, 1),
        new NotesProperties.Auth("client-id", "secret-that-is-at-least-32-bytes-long!!"),
        new NotesProperties.Limits(5_242_880),
        new NotesProperties.Mail(false, "noreply@example.com", "owner@example.com"),
        new NotesProperties.Polar(
            enabled, "https://sandbox-api.polar.sh", "token", "whsec", "prod-1", "https://x/ok"),
        new NotesProperties.Images(5_242_880, 104_857_600, ""));
  }

  @BeforeEach
  void setUp() {
    users = new InMemoryUserRepository();
    users.save(new User("u1", "ada@example.com", "Ada", false, 0, List.of()));
    PolarClient fakeClient =
        (userId, email) -> {
          checkoutRequestedFor = userId + ":" + email;
          return "https://polar.test/checkout/abc";
        };
    service = new BillingService(fakeClient, users, new ObjectMapper(), props(true));
  }

  @Test
  void createCheckoutReturnsHostedUrl() {
    String url = service.createCheckout("u1");

    assertThat(url).isEqualTo("https://polar.test/checkout/abc");
    assertThat(checkoutRequestedFor).isEqualTo("u1:ada@example.com");
  }

  @Test
  void createCheckoutFailsWhenBillingDisabled() {
    BillingService disabled =
        new BillingService((userId, email) -> "unused", users, new ObjectMapper(), props(false));

    assertThatThrownBy(() -> disabled.createCheckout("u1"))
        .isInstanceOf(BillingUnavailableException.class);
  }

  @Test
  void orderPaidActivatesPremium() {
    service.handleWebhookEvent(
        "{\"type\":\"order.paid\",\"data\":{\"customer\":{\"external_id\":\"u1\"}}}");

    assertThat(users.findById("u1").orElseThrow().premium()).isTrue();
  }

  @Test
  void otherEventTypesAreIgnored() {
    service.handleWebhookEvent(
        "{\"type\":\"order.refunded\",\"data\":{\"customer\":{\"external_id\":\"u1\"}}}");

    assertThat(users.findById("u1").orElseThrow().premium()).isFalse();
  }

  @Test
  void unknownUserAndMalformedPayloadsAreIgnoredQuietly() {
    assertThatCode(
            () -> {
              service.handleWebhookEvent(
                  "{\"type\":\"order.paid\",\"data\":{\"customer\":{\"external_id\":\"ghost\"}}}");
              service.handleWebhookEvent("{\"type\":\"order.paid\",\"data\":{}}");
              service.handleWebhookEvent("not json at all");
            })
        .doesNotThrowAnyException();
    assertThat(users.findById("u1").orElseThrow().premium()).isFalse();
  }
}
