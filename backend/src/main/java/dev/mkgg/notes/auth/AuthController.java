package dev.mkgg.notes.auth;

import dev.mkgg.notes.auth.dto.AuthResponse;
import dev.mkgg.notes.auth.dto.GoogleSignInRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sign-in endpoint. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/google")
  public AuthResponse google(@Valid @RequestBody GoogleSignInRequest request) {
    return authService.signIn(request.idToken());
  }
}
