package dev.mkgg.notes.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.mkgg.notes.common.InvalidTokenException;
import dev.mkgg.notes.config.NotesProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private final JwtService jwtService =
      new JwtService(props("secret-that-is-at-least-32-bytes-long!!"));

  private static NotesProperties props(String secret) {
    return new NotesProperties(
        new NotesProperties.Cors(List.of()),
        new NotesProperties.Aws("r", "t", "b", "s", "u"),
        new NotesProperties.RateLimit(1, 1, 1, 1, 1),
        new NotesProperties.Auth("client-id", secret),
        new NotesProperties.Limits(5_242_880));
  }

  @Test
  void issuedTokenVerifiesBackToTheUserId() {
    String token = jwtService.issue("user-123", "ada@example.com");
    assertThat(jwtService.verify(token)).isEqualTo("user-123");
  }

  @Test
  void tamperedTokenIsRejected() {
    String token = jwtService.issue("user-123", "ada@example.com");
    String tampered = token.substring(0, token.length() - 2) + "xy";
    assertThatThrownBy(() -> jwtService.verify(tampered)).isInstanceOf(InvalidTokenException.class);
  }

  @Test
  void tokenSignedWithADifferentSecretIsRejected() {
    String token = jwtService.issue("user-123", "ada@example.com");
    JwtService other = new JwtService(props("a-completely-different-secret-32-bytes!!"));
    assertThatThrownBy(() -> other.verify(token)).isInstanceOf(InvalidTokenException.class);
  }

  @Test
  void garbageIsRejected() {
    assertThatThrownBy(() -> jwtService.verify("not-a-jwt"))
        .isInstanceOf(InvalidTokenException.class);
  }
}
