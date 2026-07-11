package dev.mkgg.notes.auth;

import dev.mkgg.notes.auth.dto.AuthResponse;
import dev.mkgg.notes.auth.dto.GoogleSignInRequest;
import dev.mkgg.notes.common.InvalidTokenException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sign-in and current-account endpoints. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private static final String BEARER = "Bearer ";

  private final AuthService authService;
  private final JwtService jwtService;

  public AuthController(AuthService authService, JwtService jwtService) {
    this.authService = authService;
    this.jwtService = jwtService;
  }

  @PostMapping("/google")
  public AuthResponse google(@Valid @RequestBody GoogleSignInRequest request) {
    return authService.signIn(request.idToken());
  }

  /** Current account snapshot; lets the client refresh premium status after a purchase. */
  @GetMapping("/me")
  public MeResponse me(@RequestHeader(value = "Authorization", required = false) String auth) {
    if (auth == null || !auth.startsWith(BEARER)) {
      throw new InvalidTokenException();
    }
    User user = authService.getUser(jwtService.verify(auth.substring(BEARER.length())));
    return new MeResponse(user.email(), user.name(), user.premium());
  }

  /**
   * The signed-in account's profile.
   *
   * @param email the user's email
   * @param name display name, or {@code null}
   * @param premium whether the user has purchased premium
   */
  public record MeResponse(String email, String name, boolean premium) {}
}
