package dev.mkgg.notes.common;

import dev.mkgg.notes.config.NotesProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-client-IP token-bucket rate limiting for the abuse-prone endpoints: note creation and
 * password unlock attempts. Buckets are in-memory, which is sufficient for a single instance;
 * switch to a distributed store if the API ever scales horizontally.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final Pattern UNLOCK_PATH = Pattern.compile("^/api/notes/[^/]+/unlock/?$");
  private static final Pattern NOTE_PATH = Pattern.compile("^/api/notes/[^/]+/?$");
  private static final Pattern SUBMIT_PATH = Pattern.compile("^/api/(contact|reports)/?$");
  private static final String CREATE_PATH = "/api/notes";

  private final NotesProperties properties;
  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  public RateLimitFilter(NotesProperties properties) {
    this.properties = properties;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Bucket bucket = bucketFor(request);
    if (bucket != null && !bucket.tryConsume(1)) {
      reject(response);
      return;
    }
    filterChain.doFilter(request, response);
  }

  private Bucket bucketFor(HttpServletRequest request) {
    String method = request.getMethod();
    String path = request.getRequestURI();
    String ip = clientIp(request);
    if ("POST".equals(method)) {
      if (CREATE_PATH.equals(path) || (CREATE_PATH + "/").equals(path)) {
        return buckets.computeIfAbsent(
            "create:" + ip,
            key -> newBucket(properties.rateLimit().createPerHour(), Duration.ofHours(1)));
      }
      if (UNLOCK_PATH.matcher(path).matches()) {
        return buckets.computeIfAbsent(
            "unlock:" + ip,
            key -> newBucket(properties.rateLimit().unlockPerMinute(), Duration.ofMinutes(1)));
      }
      if (SUBMIT_PATH.matcher(path).matches()) {
        return buckets.computeIfAbsent(
            "submit:" + ip,
            key -> newBucket(properties.rateLimit().submitPerHour(), Duration.ofHours(1)));
      }
      return null;
    }
    if (("PUT".equals(method) || "DELETE".equals(method)) && NOTE_PATH.matcher(path).matches()) {
      return buckets.computeIfAbsent(
          "mutate:" + ip,
          key -> newBucket(properties.rateLimit().mutatePerMinute(), Duration.ofMinutes(1)));
    }
    return null;
  }

  private static Bucket newBucket(int permits, Duration period) {
    return Bucket.builder()
        .addLimit(Bandwidth.builder().capacity(permits).refillGreedy(permits, period).build())
        .build();
  }

  /** Uses the first {@code X-Forwarded-For} hop when behind a proxy or CDN. */
  private static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private static void reject(HttpServletResponse response) throws IOException {
    response.setStatus(429);
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response
        .getWriter()
        .write(
            "{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,"
                + "\"detail\":\"Too many requests. Please try again later.\"}");
  }
}
