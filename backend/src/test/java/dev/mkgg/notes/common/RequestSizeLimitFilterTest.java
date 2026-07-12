package dev.mkgg.notes.common;

import static org.assertj.core.api.Assertions.assertThat;

import dev.mkgg.notes.config.NotesProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestSizeLimitFilterTest {

  private static final long MAX_BYTES = 1_000;

  private RequestSizeLimitFilter filter;

  @BeforeEach
  void setUp() {
    NotesProperties properties =
        new NotesProperties(
            new NotesProperties.Cors(List.of()),
            new NotesProperties.Aws(
                "us-east-1", "notes", "notes-content", "notes-submissions", "notes-users"),
            new NotesProperties.RateLimit(2, 2, 2, 2, 1),
            new NotesProperties.Auth("client-id", "test-secret-at-least-32-characters-long"),
            new NotesProperties.Limits(MAX_BYTES),
            new NotesProperties.Mail(false, "noreply@example.com", "owner@example.com"),
            new NotesProperties.Polar(false, "https://sandbox-api.polar.sh", "", "", "", ""),
            new NotesProperties.Images(5_242_880, 104_857_600, "", ""),
            new NotesProperties.Admin(""));
    filter = new RequestSizeLimitFilter(properties);
  }

  private MockHttpServletResponse send(int contentLength) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/notes");
    if (contentLength >= 0) {
      request.setContentType("application/json");
      request.setContent(new byte[contentLength]);
    }
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    return response;
  }

  @Test
  void allowsBodiesUpToTheLimit() throws Exception {
    assertThat(send((int) MAX_BYTES).getStatus()).isEqualTo(200);
  }

  @Test
  void rejectsBodiesOverTheLimit() throws Exception {
    MockHttpServletResponse response = send((int) MAX_BYTES + 1);
    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getContentAsString()).contains("Payload Too Large");
  }

  @Test
  void allowsRequestsWithoutABody() throws Exception {
    assertThat(send(-1).getStatus()).isEqualTo(200);
  }
}
