package dev.mkgg.notes.image;

import dev.mkgg.notes.config.NotesProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Stores images in the content bucket; CloudFront serves them from the configured public base URL
 * ({@code https://<site-domain>/images/...}).
 */
@Component
@Profile("aws")
public class S3ImageStore implements ImageStore {

  private final S3Client s3;
  private final String bucket;
  private final String publicBaseUrl;

  public S3ImageStore(S3Client s3, NotesProperties properties) {
    this.s3 = s3;
    this.bucket = properties.aws().bucket();
    this.publicBaseUrl = properties.images().publicBaseUrl();
  }

  @Override
  public String store(String key, byte[] bytes, String contentType) {
    s3.putObject(
        request ->
            request
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                // Immutable content under a random key: cache hard at the edge.
                .cacheControl("public, max-age=31536000, immutable"),
        RequestBody.fromBytes(bytes));
    return publicBaseUrl + "/" + key;
  }
}
