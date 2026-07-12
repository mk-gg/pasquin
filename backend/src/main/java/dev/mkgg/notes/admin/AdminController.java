package dev.mkgg.notes.admin;

import dev.mkgg.notes.auth.JwtService;
import dev.mkgg.notes.common.AccessDeniedException;
import dev.mkgg.notes.common.InvalidTokenException;
import dev.mkgg.notes.config.NotesProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operator-only moderation endpoints. The caller must be signed in as the single configured admin
 * account; with no admin configured the endpoints reject everyone.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private static final String BEARER = "Bearer ";

  private final AdminService adminService;
  private final JwtService jwtService;
  private final NotesProperties.Admin admin;

  public AdminController(
      AdminService adminService, JwtService jwtService, NotesProperties properties) {
    this.adminService = adminService;
    this.jwtService = jwtService;
    this.admin = properties.admin();
  }

  /** Takes down a reported note: metadata, content, embedded images, and CDN caches. */
  @DeleteMapping("/notes/{slug}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void takeDownNote(
      @RequestHeader(value = "Authorization", required = false) String auth,
      @PathVariable String slug) {
    requireAdmin(auth);
    adminService.takeDownNote(slug);
  }

  private void requireAdmin(String authHeader) {
    if (authHeader == null || !authHeader.startsWith(BEARER)) {
      throw new InvalidTokenException();
    }
    String userId = jwtService.verify(authHeader.substring(BEARER.length()));
    if (admin.userId() == null || admin.userId().isBlank() || !admin.userId().equals(userId)) {
      throw new AccessDeniedException();
    }
  }
}
