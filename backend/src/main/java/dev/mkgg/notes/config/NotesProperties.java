package dev.mkgg.notes.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application configuration bound from the {@code notes.*} keys in {@code application.yml}.
 *
 * @param cors CORS settings for the frontend origins
 * @param aws names of the AWS resources used by the {@code aws} profile
 * @param rateLimit per-client request limits for abuse-prone endpoints
 */
@ConfigurationProperties(prefix = "notes")
public record NotesProperties(Cors cors, Aws aws, RateLimit rateLimit) {

  /**
   * Per-client rate limits.
   *
   * @param createPerHour note creations allowed per client IP per hour
   * @param unlockPerMinute unlock attempts allowed per client IP per minute
   * @param mutatePerMinute updates/deletes allowed per client IP per minute (autosave traffic)
   * @param submitPerHour contact/report submissions allowed per client IP per hour
   */
  public record RateLimit(
      int createPerHour, int unlockPerMinute, int mutatePerMinute, int submitPerHour) {}

  /**
   * CORS settings.
   *
   * @param allowedOrigins origins allowed to call the API
   */
  public record Cors(List<String> allowedOrigins) {}

  /**
   * AWS resource names.
   *
   * @param region AWS region, e.g. {@code ap-southeast-1}
   * @param tableName DynamoDB table holding note metadata
   * @param bucket S3 bucket holding note content
   * @param submissionsTable DynamoDB table holding contact/report submissions
   */
  public record Aws(String region, String tableName, String bucket, String submissionsTable) {}
}
