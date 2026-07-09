package dev.mkgg.notes.submission;

import dev.mkgg.notes.submission.dto.ContactRequest;
import dev.mkgg.notes.submission.dto.ReportRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints for the public contact and abuse-report forms. */
@RestController
@RequestMapping("/api")
public class SubmissionController {

  private final SubmissionService submissionService;

  public SubmissionController(SubmissionService submissionService) {
    this.submissionService = submissionService;
  }

  @PostMapping("/contact")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void contact(@Valid @RequestBody ContactRequest request) {
    submissionService.submitContact(request);
  }

  @PostMapping("/reports")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void report(@Valid @RequestBody ReportRequest request) {
    submissionService.submitReport(request);
  }
}
