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
            new NotesProperties.RateLimit(2, 2, 2, 2),
            new NotesProperties.Auth("client-id", "test-secret-at-least-32-characters-long"));
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

  @Test
  void usesFirstForwardedForHopBehindProxy() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/notes");
    request.setRequestURI("/api/notes");
    request.addHeader("X-Forwarded-For", "9.9.9.9, 10.0.0.1");
    request.setRemoteAddr("10.0.0.1");
    for (int i = 0; i < 2; i++) {
      MockHttpServletResponse response = new MockHttpServletResponse();
      MockHttpServletRequest r = new MockHttpServletRequest("POST", "/api/notes");
      r.setRequestURI("/api/notes");
      r.addHeader("X-Forwarded-For", "9.9.9.9, 10.0.0.1");
      r.setRemoteAddr("10.0.0.1");
      filter.doFilter(r, response, new MockFilterChain());
      assertThat(response.getStatus()).isEqualTo(200);
    }
    // same forwarded IP from a different proxy hop is still the same client
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockHttpServletRequest r = new MockHttpServletRequest("POST", "/api/notes");
    r.setRequestURI("/api/notes");
    r.addHeader("X-Forwarded-For", "9.9.9.9, 10.0.0.2");
    r.setRemoteAddr("10.0.0.2");
    filter.doFilter(r, response, new MockFilterChain());
    assertThat(response.getStatus()).isEqualTo(429);
  }
}
