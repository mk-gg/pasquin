package dev.mkgg.notes.auth;

import dev.mkgg.notes.common.InvalidTokenException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** A signed-in user's synced note list. All endpoints require a Bearer session token. */
@RestController
@RequestMapping("/api/me/notes")
public class UserNotesController {

  private static final String BEARER = "Bearer ";

  /** A synced account holding more notes than this is implausible; caps merge payloads. */
  private static final int MAX_MERGE_NOTES = 500;

  private final AuthService authService;
  private final JwtService jwtService;

  public UserNotesController(AuthService authService, JwtService jwtService) {
    this.authService = authService;
    this.jwtService = jwtService;
  }

  @GetMapping
  public List<OwnedNote> list(
      @RequestHeader(value = "Authorization", required = false) String auth) {
    return authService.getNotes(userId(auth));
  }

  @PutMapping
  public void put(
      @RequestHeader(value = "Authorization", required = false) String auth,
      @Valid @RequestBody OwnedNote note) {
    authService.putNote(userId(auth), note);
  }

  @PostMapping("/merge")
  public List<OwnedNote> merge(
      @RequestHeader(value = "Authorization", required = false) String auth,
      @Valid @RequestBody List<@Valid OwnedNote> notes) {
    if (notes.size() > MAX_MERGE_NOTES) {
      throw new IllegalArgumentException("Too many notes in a single merge");
    }
    return authService.mergeNotes(userId(auth), notes);
  }

  @DeleteMapping("/{slug}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @RequestHeader(value = "Authorization", required = false) String auth,
      @PathVariable String slug) {
    authService.deleteNote(userId(auth), slug);
  }

  private String userId(String authHeader) {
    if (authHeader == null || !authHeader.startsWith(BEARER)) {
      throw new InvalidTokenException();
    }
    return jwtService.verify(authHeader.substring(BEARER.length()));
  }
}
