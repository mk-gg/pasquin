package dev.mkgg.notes.common;

import static org.assertj.core.api.Assertions.assertThat;

import dev.mkgg.notes.config.NotesProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

  private RateLimitFilter filter;

  @BeforeEach
  void setUp() {
    NotesProperties properties =
        new NotesProperties(
            new NotesProperties.Cors(List.of()),
            new NotesProperties.Aws(
                "us-east-1", "notes", "notes-content", "notes-submissions", "notes-users"),
            new NotesProperties.RateLimit(2, 2, 2, 2, 1),
            new NotesProperties.Auth("client-id", "test-secret-at-least-32-characters-long"),
            new NotesProperties.Limits(5_242_880),
            new NotesProperties.Mail(false, "noreply@example.com", "owner@example.com"),
            new NotesProperties.Polar(false, "https://sandbox-api.polar.sh", "", "", "", ""),
            new NotesProperties.Images(5_242_880, 104_857_600, "", ""),
            new NotesProperties.Admin(""));
    filter = new RateLimitFilter(properties);
  }

  private MockHttpServletResponse send(String method, String path, String ip) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    request.setRequestURI(path);
    request.setRemoteAddr(ip);
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    return response;
  }

  @Test
  void allowsCreatesUpToTheLimitThenRejects() throws Exception {
    assertThat(send("POST", "/api/notes", "1.1.1.1").getStatus()).isEqualTo(200);
    assertThat(send("POST", "/api/notes", "1.1.1.1").getStatus()).isEqualTo(200);
    assertThat(send("POST", "/api/notes", "1.1.1.1").getStatus()).isEqualTo(429);
  }

  @Test
  void limitsAreTrackedPerIp() throws Exception {
    send("POST", "/api/notes", "1.1.1.1");
    send("POST", "/api/notes", "1.1.1.1");
    assertThat(send("POST", "/api/notes", "2.2.2.2").getStatus()).isEqualTo(200);
  }

  @Test
  void unlockAttemptsAreLimitedSeparately() throws Exception {
    send("POST", "/api/notes", "1.1.1.1");
    send("POST", "/api/notes", "1.1.1.1");
    assertThat(send("POST", "/api/notes/abc123/unlock", "1.1.1.1").getStatus()).isEqualTo(200);
    send("POST", "/api/notes/abc123/unlock", "1.1.1.1");
    assertThat(send("POST", "/api/notes/abc123/unlock", "1.1.1.1").getStatus()).isEqualTo(429);
  }

  @Test
  void contactAndReportsShareTheSubmitLimit() throws Exception {
    assertThat(send("POST", "/api/contact", "1.1.1.1").getStatus()).isEqualTo(200);
    assertThat(send("POST", "/api/reports", "1.1.1.1").getStatus()).isEqualTo(200);
    assertThat(send("POST", "/api/contact", "1.1.1.1").getStatus()).isEqualTo(429);
  }

  @Test
  void updatesAndDeletesShareTheMutateLimit() throws Exception {
    assertThat(send("PUT", "/api/notes/abc123", "1.1.1.1").getStatus()).isEqualTo(200);
    assertThat(send("DELETE", "/api/notes/abc123", "1.1.1.1").getStatus()).isEqualTo(200);
    assertThat(send("PUT", "/api/notes/abc123", "1.1.1.1").getStatus()).isEqualTo(429);
  }

  @Test
  void readsAreNeverLimited() throws Exception {
    for (int i = 0; i < 10; i++) {
      assertThat(send("GET", "/api/notes/abc123", "1.1.1.1").getStatus()).isEqualTo(200);
    }
  }

  private MockHttpServletResponse sendForwarded(String forwardedFor) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/notes");
    request.setRequestURI("/api/notes");
    request.addHeader("X-Forwarded-For", forwardedFor);
    request.setRemoteAddr("10.0.0.1"); // App Runner's internal address
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    return response;
  }

  @Test
  void usesTheRealClientIpFromTheTrustedRightmostHop() throws Exception {
    // App Runner appends the real client IP, so it is the rightmost entry.
    assertThat(sendForwarded("203.0.113.5").getStatus()).isEqualTo(200);
    assertThat(sendForwarded("203.0.113.5").getStatus()).isEqualTo(200);
    assertThat(sendForwarded("203.0.113.5").getStatus()).isEqualTo(429);
  }

  @Test
  void spoofedLeadingForwardedForHopsCannotForgeFreshBuckets() throws Exception {
    // Same real client (rightmost) but a different attacker-chosen leading hop
    // on every request: all must share one bucket and hit the limit.
    assertThat(sendForwarded("1.1.1.1, 203.0.113.9").getStatus()).isEqualTo(200);
    assertThat(sendForwarded("2.2.2.2, 203.0.113.9").getStatus()).isEqualTo(200);
    assertThat(sendForwarded("3.3.3.3, 203.0.113.9").getStatus()).isEqualTo(429);
  }
}
