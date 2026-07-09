package dev.mkgg.notes.submission.storage;

import dev.mkgg.notes.submission.Submission;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/** List-backed submission store for local development without AWS credentials. */
@Repository
@Profile("!aws")
public class InMemorySubmissionRepository implements SubmissionRepository {

  private final List<Submission> submissions = new CopyOnWriteArrayList<>();

  @Override
  public void save(Submission submission) {
    submissions.add(submission);
  }

  /** All stored submissions; used for verification in tests. */
  public List<Submission> findAll() {
    return List.copyOf(submissions);
  }
}
