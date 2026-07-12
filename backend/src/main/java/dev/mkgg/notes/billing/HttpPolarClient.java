package dev.mkgg.notes.billing;

import dev.mkgg.notes.config.NotesProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** Polar REST client backed by the JDK HTTP client. */
@Component
public class HttpPolarClient implements PolarClient {

  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  private final HttpClient http;
  private final ObjectMapper objectMapper;
  private final NotesProperties.Polar polar;

  public HttpPolarClient(ObjectMapper objectMapper, NotesProperties properties) {
    this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    this.objectMapper = objectMapper;
    this.polar = properties.polar();
  }

  @Override
  public String createCheckoutUrl(String userId, String email) {
    ObjectNode body = objectMapper.createObjectNode();
    body.putArray("products").add(polar.productId());
    body.put("external_customer_id", userId);
    body.put("customer_email", email);
    body.put("success_url", polar.successUrl());

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(polar.apiBase() + "/v1/checkouts/"))
            .timeout(TIMEOUT)
            .header("Authorization", "Bearer " + polar.accessToken())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
    try {
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() / 100 != 2) {
        String detail = response.body() == null ? "" : response.body();
        throw new BillingUnavailableException(
            "Polar checkout creation failed with status "
                + response.statusCode()
                + ": "
                + detail.substring(0, Math.min(detail.length(), 500)));
      }
      JsonNode url = objectMapper.readTree(response.body()).path("url");
      if (!url.isTextual()) {
        throw new BillingUnavailableException("Polar checkout response had no url");
      }
      return url.asText();
    } catch (IOException e) {
      throw new BillingUnavailableException("Polar checkout request failed", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new BillingUnavailableException("Polar checkout request interrupted", e);
    }
  }
}
