package dev.mkgg.notes.note.storage;

import dev.mkgg.notes.config.NotesProperties;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** S3-backed content store; each note body is a single JSON object under {@code notes/}. */
@Component
@Profile("aws")
public class S3NoteContentStore implements NoteContentStore {

  private final S3Client s3;
  private final String bucket;

  public S3NoteContentStore(S3Client s3, NotesProperties properties) {
    this.s3 = s3;
    this.bucket = properties.aws().bucket();
  }

  @Override
  public void put(String slug, String contentJson) {
    PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(key(slug))
            .contentType("application/json")
            .build();
    s3.putObject(request, RequestBody.fromString(contentJson, StandardCharsets.UTF_8));
  }

  @Override
  public Optional<String> get(String slug) {
    GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(key(slug)).build();
    try {
      return Optional.of(s3.getObjectAsBytes(request).asUtf8String());
    } catch (NoSuchKeyException e) {
      return Optional.empty();
    }
  }

  @Override
  public void delete(String slug) {
    s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key(slug)).build());
  }

  private static String key(String slug) {
    return "notes/" + slug + ".json";
  }
}
