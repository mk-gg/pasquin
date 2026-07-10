package dev.mkgg.notes.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import dev.mkgg.notes.common.InvalidTokenException;
import dev.mkgg.notes.config.NotesProperties;
import java.net.URI;
import java.util.Date;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Verifies Google ID tokens: validates the RS256 signature against Google's published keys (JWKS,
 * cached), and checks the issuer, audience (our client id), and expiry.
 */
@Component
public class GoogleTokenVerifier {

  private static final String JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";
  private static final Set<String> ISSUERS =
      Set.of("https://accounts.google.com", "accounts.google.com");

  private final ConfigurableJWTProcessor<SecurityContext> processor;
  private final String clientId;

  public GoogleTokenVerifier(NotesProperties properties) {
    this.clientId = properties.auth().googleClientId();
    try {
      JWKSource<SecurityContext> keySource =
          JWKSourceBuilder.create(URI.create(JWKS_URL).toURL()).build();
      DefaultJWTProcessor<SecurityContext> p = new DefaultJWTProcessor<>();
      p.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource));
      this.processor = p;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to initialize Google token verifier", e);
    }
  }

  public GoogleUser verify(String idToken) {
    try {
      JWTClaimsSet claims = processor.process(idToken, null);
      if (!ISSUERS.contains(claims.getIssuer())) {
        throw new InvalidTokenException();
      }
      if (claims.getAudience() == null || !claims.getAudience().contains(clientId)) {
        throw new InvalidTokenException();
      }
      Date expiry = claims.getExpirationTime();
      if (expiry == null || expiry.before(new Date())) {
        throw new InvalidTokenException();
      }
      String sub = claims.getSubject();
      String email = claims.getStringClaim("email");
      if (sub == null || email == null) {
        throw new InvalidTokenException();
      }
      return new GoogleUser(
          sub, email, claims.getStringClaim("name"), claims.getStringClaim("picture"));
    } catch (InvalidTokenException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidTokenException();
    }
  }
}
