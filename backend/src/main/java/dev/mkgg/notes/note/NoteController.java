package dev.mkgg.notes.note;

import dev.mkgg.notes.note.dto.CreateNoteRequest;
import dev.mkgg.notes.note.dto.CreateNoteResponse;
import dev.mkgg.notes.note.dto.NoteResponse;
import dev.mkgg.notes.note.dto.UnlockNoteRequest;
import dev.mkgg.notes.note.dto.UpdateNoteRequest;
import jakarta.validation.Valid;
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

/** REST endpoints for creating and reading notes. */
@RestController
@RequestMapping("/api/notes")
public class NoteController {

  private final NoteService noteService;

  public NoteController(NoteService noteService) {
    this.noteService = noteService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CreateNoteResponse create(@Valid @RequestBody CreateNoteRequest request) {
    return noteService.create(request);
  }

  @GetMapping("/{slug}")
  public NoteResponse get(
      @PathVariable String slug,
      @RequestHeader(value = "X-Edit-Key", required = false) String editKey) {
    return noteService.get(slug, editKey);
  }

  @PostMapping("/{slug}/unlock")
  public NoteResponse unlock(
      @PathVariable String slug, @Valid @RequestBody UnlockNoteRequest request) {
    return noteService.unlock(slug, request.password());
  }

  @PutMapping("/{slug}")
  public NoteResponse update(
      @PathVariable String slug,
      @RequestHeader(value = "X-Edit-Key", required = false) String editKey,
      @Valid @RequestBody UpdateNoteRequest request) {
    return noteService.update(slug, editKey, request);
  }

  @DeleteMapping("/{slug}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @PathVariable String slug,
      @RequestHeader(value = "X-Edit-Key", required = false) String editKey) {
    noteService.delete(slug, editKey);
  }
}
