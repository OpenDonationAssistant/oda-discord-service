package io.github.opendonationassistant.integration.webhook;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.Optional;

@Singleton
public class WebhookClient {

  private final ODALogger log = new ODALogger(this);
  private final HttpClient httpClient;

  @Inject
  public WebhookClient(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public void sendMessage(String url, String message) {
    try {
      var response = httpClient
        .toBlocking()
        .exchange(
          HttpRequest
            .POST(url, new DiscordWebhookPayload(message))
            .contentType(MediaType.APPLICATION_JSON),
          String.class
        );
      log.info(
        "Webhook message sent",
        Map.of("url", url, "status", response.getStatus().getCode())
      );
    } catch (HttpClientException e) {
      log.error(
        "Failed to send webhook message",
        Map.of(
          "url",
          url,
          "error",
          Optional.ofNullable(e.getMessage()).orElse("")
        )
      );
    }
  }

  @Serdeable
  public record DiscordWebhookPayload(String content) {}
}
