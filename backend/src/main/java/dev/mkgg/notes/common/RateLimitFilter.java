package dev.mkgg.notes.common;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.mkgg.notes.config.NotesProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-client-IP token-bucket rate limiting for the abuse-prone endpoints: note creation and
 * password unlock attempts. Buckets are in-memory, which is sufficient for a single instance;
 * switch to a distributed store if the API ever scales horizontally.
 *
 * <p>Buckets live in a bounded, self-expiring cache so a flood of distinct client IPs cannot grow
 * the map without limit and exhaust heap. Idle buckets are evicted after they would have refilled
 * anyway, so eviction never lets an abuser off early.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final Pattern UNLOCK_PATH = Pattern.compile("^/api/notes/[^/]+/unlock/?$");
  private static final Pattern NOTE_PATH = Pattern.compile("^/api/notes/[^/]+/?$");
  private static final Pattern SUBMIT_PATH = Pattern.compile("^/api/(contact|reports)/?$");
  private static final String CREATE_PATH = "/api/notes";

  // Cap the number of tracked clients; longest bucket window is one hour, so an
  // idle bucket has fully refilled by the time it is evicted.
  private static final long MAX_TRACKED_CLIENTS = 100_000;
  private static final Duration BUCKET_TTL = Duration.ofHours(1);

  private final NotesProperties properties;
  private final Cache<String, Bucket> buckets =
      Caffeine.newBuilder().maximumSize(MAX_TRACKED_CLIENTS).expireAfterAccess(BUCKET_TTL).build();

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
    String ip = clientIp(request, properties.rateLimit().trustedProxyHops());
    if ("POST".equals(method)) {
      if (CREATE_PATH.equals(path) || (CREATE_PATH + "/").equals(path)) {
        return buckets.get(
            "create:" + ip,
            key -> newBucket(properties.rateLimit().createPerHour(), Duration.ofHours(1)));
      }
      if (UNLOCK_PATH.matcher(path).matches()) {
        return buckets.get(
            "unlock:" + ip,
            key -> newBucket(properties.rateLimit().unlockPerMinute(), Duration.ofMinutes(1)));
      }
      if (SUBMIT_PATH.matcher(path).matches()) {
        return buckets.get(
            "submit:" + ip,
            key -> newBucket(properties.rateLimit().submitPerHour(), Duration.ofHours(1)));
      }
      return null;
    }
    if (("PUT".equals(method) || "DELETE".equals(method)) && NOTE_PATH.matcher(path).matches()) {
      return buckets.get(
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

  /**
   * Resolves the client IP for rate-limit keying.
   *
   * <p>{@code X-Forwarded-For} is {@code client, proxy1, ..., proxyN}, appended left-to-right, so
   * only the rightmost {@code trustedHops} entries come from our own infrastructure. Reading the
   * client IP that many entries from the right ignores any values the client injects on the left —
   * otherwise an attacker could send a random leading hop per request to get a fresh bucket every
   * time and defeat the limit entirely (including the unlock brute-force protection).
   */
  private static String clientIp(HttpServletRequest request, int trustedHops) {
    if (trustedHops > 0) {
      String forwarded = request.getHeader("X-Forwarded-For");
      if (forwarded != null && !forwarded.isBlank()) {
        String[] hops = forwarded.split(",");
        int index = hops.length - trustedHops;
        if (index >= 0) {
          String ip = hops[index].trim();
          if (!ip.isBlank()) {
            return ip;
          }
        }
      }
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
