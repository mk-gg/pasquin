package dev.mkgg.notes.auth;

import dev.mkgg.notes.auth.dto.AuthResponse;
import dev.mkgg.notes.auth.storage.UserRepository;
import dev.mkgg.notes.common.InvalidTokenException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Sign-in and per-user note-list management. */
@Service
public class AuthService {

  private final GoogleTokenVerifier googleTokenVerifier;
  private final JwtService jwtService;
  private final UserRepository userRepository;

  public AuthService(
      GoogleTokenVerifier googleTokenVerifier,
      JwtService jwtService,
      UserRepository userRepository) {
    this.googleTokenVerifier = googleTokenVerifier;
    this.jwtService = jwtService;
    this.userRepository = userRepository;
  }

  /** Verifies a Google ID token, upserts the user, and issues our session token. */
  public AuthResponse signIn(String idToken) {
    GoogleUser google = googleTokenVerifier.verify(idToken);
    User user =
        userRepository
            .findById(google.sub())
            .map(existing -> existing.withProfile(google.email(), google.name()))
            .orElseGet(() -> new User(google.sub(), google.email(), google.name(), List.of()));
    userRepository.save(user);
    String token = jwtService.issue(user.id(), user.email());
    return new AuthResponse(token, user.email(), user.name(), google.picture());
  }

  public List<OwnedNote> getNotes(String userId) {
    return userRepository.findById(userId).map(User::notes).orElseGet(List::of);
  }

  /** Adds or updates a single note in the user's list. */
  public void putNote(String userId, OwnedNote note) {
    User user = requireUser(userId);
    List<OwnedNote> notes = new ArrayList<>(user.notes());
    notes.removeIf(existing -> existing.slug().equals(note.slug()));
    notes.add(note);
    userRepository.save(user.withNotes(List.copyOf(notes)));
  }

  /** Merges a batch of notes into the user's list (used on sign-in); keeps existing on conflict. */
  public List<OwnedNote> mergeNotes(String userId, List<OwnedNote> incoming) {
    User user = requireUser(userId);
    Map<String, OwnedNote> bySlug = new LinkedHashMap<>();
    for (OwnedNote note : user.notes()) {
      bySlug.put(note.slug(), note);
    }
    for (OwnedNote note : incoming) {
      bySlug.putIfAbsent(note.slug(), note);
    }
    List<OwnedNote> merged = List.copyOf(bySlug.values());
    userRepository.save(user.withNotes(merged));
    return merged;
  }

  public void deleteNote(String userId, String slug) {
    User user = requireUser(userId);
    List<OwnedNote> notes =
        user.notes().stream().filter(note -> !note.slug().equals(slug)).toList();
    userRepository.save(user.withNotes(notes));
  }

  private User requireUser(String userId) {
    return userRepository.findById(userId).orElseThrow(InvalidTokenException::new);
  }
}
