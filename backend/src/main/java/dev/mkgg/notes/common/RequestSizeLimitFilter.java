package dev.mkgg.notes.common;

import dev.mkgg.notes.config.NotesProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects requests whose body exceeds the configured limit, based on the {@code Content-Length}
 * header, before Spring buffers and parses the body. This caps note content and account-sync
 * payloads so a huge request cannot exhaust heap during JSON parsing or bloat stored objects.
 *
 * <p>Runs early so an oversized request is turned away before any downstream work. A request with
 * no {@code Content-Length} (e.g. chunked) is passed through; per-field size constraints and the
 * per-client rate limit remain as backstops for that narrower case.
 */
@Component
@Order(-100)
public class RequestSizeLimitFilter extends OncePerRequestFilter {

  private final long maxRequestBytes;

  public RequestSizeLimitFilter(NotesProperties properties) {
    this.maxRequestBytes = properties.limits().maxRequestBytes();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long contentLength = request.getContentLengthLong();
    if (contentLength > maxRequestBytes) {
      reject(response);
      return;
    }
    filterChain.doFilter(request, response);
  }

  private static void reject(HttpServletResponse response) throws IOException {
    response.setStatus(413);
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response
        .getWriter()
        .write(
            "{\"type\":\"about:blank\",\"title\":\"Payload Too Large\",\"status\":413,"
                + "\"detail\":\"Request body is too large.\"}");
  }
}
