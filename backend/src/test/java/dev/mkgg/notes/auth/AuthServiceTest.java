package dev.mkgg.notes.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.mkgg.notes.auth.storage.InMemoryUserRepository;
import dev.mkgg.notes.common.InvalidTokenException;
import dev.mkgg.notes.config.NotesProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Exercises the note-list management (sign-in itself needs a live Google token). */
class AuthServiceTest {

  private InMemoryUserRepository users;
  private AuthService authService;

  private static NotesProperties props() {
    return new NotesProperties(
        new NotesProperties.Cors(List.of()),
        new NotesProperties.Aws("r", "t", "b", "s", "u"),
        new NotesProperties.RateLimit(1, 1, 1, 1, 1),
        new NotesProperties.Auth("client-id", "secret-that-is-at-least-32-bytes-long!!"),
        new NotesProperties.Limits(5_242_880),
        new NotesProperties.Mail(false, "noreply@example.com", "owner@example.com"),
        new NotesProperties.Polar(false, "https://sandbox-api.polar.sh", "", "", "", ""));
  }

  private static OwnedNote note(String slug, String title) {
    return new OwnedNote(slug, "key-" + slug, title, "2026-07-09T00:00:00Z", null);
  }

  @BeforeEach
  void setUp() {
    NotesProperties properties = props();
    users = new InMemoryUserRepository();
    authService =
        new AuthService(new GoogleTokenVerifier(properties), new JwtService(properties), users);
    users.save(new User("u1", "ada@example.com", "Ada", false, List.of()));
  }

  @Test
  void unknownUserHasNoNotes() {
    assertThat(authService.getNotes("nobody")).isEmpty();
  }

  @Test
  void putAddsAndThenUpdatesBySlug() {
    authService.putNote("u1", note("aaa", "First"));
    authService.putNote("u1", note("bbb", "Second"));
    assertThat(authService.getNotes("u1")).hasSize(2);

    authService.putNote("u1", note("aaa", "First renamed"));
    assertThat(authService.getNotes("u1")).hasSize(2);
    assertThat(authService.getNotes("u1"))
        .anyMatch(n -> n.slug().equals("aaa") && n.title().equals("First renamed"));
  }

  @Test
  void mergeKeepsExistingOnSlugConflict() {
    authService.putNote("u1", note("aaa", "Server title"));
    List<OwnedNote> merged =
        authService.mergeNotes(
            "u1", List.of(note("aaa", "Local title"), note("ccc", "New from local")));

    assertThat(merged).hasSize(2);
    assertThat(merged).anyMatch(n -> n.slug().equals("aaa") && n.title().equals("Server title"));
    assertThat(merged).anyMatch(n -> n.slug().equals("ccc"));
  }

  @Test
  void deleteRemovesBySlug() {
    authService.putNote("u1", note("aaa", "First"));
    authService.putNote("u1", note("bbb", "Second"));
    authService.deleteNote("u1", "aaa");

    assertThat(authService.getNotes("u1")).hasSize(1);
    assertThat(authService.getNotes("u1")).noneMatch(n -> n.slug().equals("aaa"));
  }

  @Test
  void mutatingAnUnknownUserIsRejected() {
    assertThatThrownBy(() -> authService.putNote("ghost", note("x", "X")))
        .isInstanceOf(InvalidTokenException.class);
  }
}
