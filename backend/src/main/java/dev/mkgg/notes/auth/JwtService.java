package dev.mkgg.notes.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import dev.mkgg.notes.common.InvalidTokenException;
import dev.mkgg.notes.config.NotesProperties;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.springframework.stereotype.Component;

/** Issues and verifies our own session tokens (HMAC-signed JWTs). */
@Component
public class JwtService {

  private static final Duration TTL = Duration.ofDays(30);

  private final byte[] secret;

  public JwtService(NotesProperties properties) {
    this.secret = properties.auth().jwtSecret().getBytes(StandardCharsets.UTF_8);
  }

  /** Issues a signed session token for the given user. */
  public String issue(String userId, String email) {
    try {
      Instant now = Instant.now();
      JWTClaimsSet claims =
          new JWTClaimsSet.Builder()
              .subject(userId)
              .claim("email", email)
              .issueTime(Date.from(now))
              .expirationTime(Date.from(now.plus(TTL)))
              .build();
      SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
      jwt.sign(new MACSigner(secret));
      return jwt.serialize();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to issue session token", e);
    }
  }

  /** Returns the user id from a valid token; throws {@link InvalidTokenException} otherwise. */
  public String verify(String token) {
    try {
      SignedJWT jwt = SignedJWT.parse(token);
      if (!jwt.verify(new MACVerifier(secret))) {
        throw new InvalidTokenException();
      }
      Date expiry = jwt.getJWTClaimsSet().getExpirationTime();
      if (expiry == null || expiry.before(new Date())) {
        throw new InvalidTokenException();
      }
      return jwt.getJWTClaimsSet().getSubject();
    } catch (InvalidTokenException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidTokenException();
    }
  }
}
