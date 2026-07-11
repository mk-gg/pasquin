package dev.mkgg.notes.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application configuration bound from the {@code notes.*} keys in {@code application.yml}.
 *
 * @param cors CORS settings for the frontend origins
 * @param aws names of the AWS resources used by the {@code aws} profile
 * @param rateLimit per-client request limits for abuse-prone endpoints
 * @param auth Google sign-in and session-token settings
 * @param limits request-size limits guarding memory and storage
 * @param mail SES notification settings for contact/report submissions
 * @param polar Polar (merchant of record) settings for premium purchases
 */
@ConfigurationProperties(prefix = "notes")
public record NotesProperties(
    Cors cors, Aws aws, RateLimit rateLimit, Auth auth, Limits limits, Mail mail, Polar polar) {

  /**
   * Polar billing settings. Premium is sold as a one-time purchase; Polar is the merchant of record
   * and confirms payment via webhook.
   *
   * @param enabled whether checkout/webhook endpoints are active
   * @param apiBase Polar API base URL ({@code https://sandbox-api.polar.sh} or {@code
   *     https://api.polar.sh})
   * @param accessToken organization access token used to create checkout sessions
   * @param webhookSecret Standard-Webhooks signing secret from the Polar webhook endpoint config
   * @param productId UUID of the premium product in Polar
   * @param successUrl where Polar redirects the customer after payment
   */
  public record Polar(
      boolean enabled,
      String apiBase,
      String accessToken,
      String webhookSecret,
      String productId,
      String successUrl) {}

  /**
   * Submission notification emails, sent via SES under the {@code aws} profile.
   *
   * @param enabled whether to send an email for each stored submission
   * @param from sender address; its domain must be a verified SES identity
   * @param to recipient address (the site owner)
   */
  public record Mail(boolean enabled, String from, String to) {}

  /**
   * Request-size limits.
   *
   * @param maxRequestBytes largest accepted request body, by {@code Content-Length}. Caps note
   *     content and account-sync payloads so an oversized body is rejected before it is buffered
   *     and parsed into memory.
   */
  public record Limits(long maxRequestBytes) {}

  /**
   * Authentication settings.
   *
   * @param googleClientId OAuth client ID; the expected audience of Google ID tokens
   * @param jwtSecret HMAC secret for signing our own session tokens (at least 32 chars)
   */
  public record Auth(String googleClientId, String jwtSecret) {}

  /**
   * Per-client rate limits.
   *
   * @param createPerHour note creations allowed per client IP per hour
   * @param unlockPerMinute unlock attempts allowed per client IP per minute
   * @param mutatePerMinute updates/deletes allowed per client IP per minute (autosave traffic)
   * @param submitPerHour contact/report submissions allowed per client IP per hour
   * @param trustedProxyHops number of trusted reverse-proxy hops that append to {@code
   *     X-Forwarded-For}. The client IP is read this many entries from the right of that header, so
   *     values a client injects on the left cannot be used to forge a fresh rate-limit bucket. Set
   *     to the number of proxies in front of the app (App Runner = 1); use 0 for direct exposure
   *     with no trusted proxy, which falls back to the socket address.
   */
  public record RateLimit(
      int createPerHour,
      int unlockPerMinute,
      int mutatePerMinute,
      int submitPerHour,
      int trustedProxyHops) {}

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
   * @param usersTable DynamoDB table holding user accounts and their note lists
   */
  public record Aws(
      String region, String tableName, String bucket, String submissionsTable, String usersTable) {}
}
