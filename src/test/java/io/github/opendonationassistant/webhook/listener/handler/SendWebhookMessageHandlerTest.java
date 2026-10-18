package io.github.opendonationassistant.webhook.listener.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.integration.webhook.WebhookClient;
import io.github.opendonationassistant.webhook.listener.handler.SendWebhookMessageHandler.SendWebhookMessageCommand;
import io.github.opendonationassistant.webhook.repository.Webhook;
import io.github.opendonationassistant.webhook.repository.WebhookDataRepository;
import io.micronaut.serde.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SendWebhookMessageHandlerTest {

  private WebhookDataRepository repository;
  private WebhookClient client;
  private SendWebhookMessageHandler handler;

  @BeforeEach
  public void setUp() {
    repository = mock(WebhookDataRepository.class);
    client = mock(WebhookClient.class);
    handler = new SendWebhookMessageHandler(
      mock(ObjectMapper.class),
      repository,
      client
    );
  }

  private static Webhook webhook(String id, String url, boolean enabled) {
    return new Webhook(id, "recipient-1", url, enabled);
  }

  private static SendWebhookMessageCommand command() {
    return new SendWebhookMessageCommand("recipient-1", "hello");
  }

  @Test
  public void sendsToAllEnabledWebhooks() throws IOException {
    when(repository.findByRecipientId("recipient-1")).thenReturn(
      List.of(
        webhook("h1", "https://example.com/1", true),
        webhook("h2", "https://example.com/2", true)
      )
    );

    handler.handle(command());

    verify(client).sendMessage("https://example.com/1", "hello");
    verify(client).sendMessage("https://example.com/2", "hello");
  }

  @Test
  public void skipsDisabledWebhooks() throws IOException {
    when(repository.findByRecipientId("recipient-1")).thenReturn(
      List.of(
        webhook("h1", "https://example.com/1", true),
        webhook("h2", "https://example.com/2", false)
      )
    );

    handler.handle(command());

    verify(client).sendMessage("https://example.com/1", "hello");
    verify(client, never()).sendMessage("https://example.com/2", "hello");
  }

  @Test
  public void doesNothingWhenNoWebhooks() throws IOException {
    when(repository.findByRecipientId("recipient-1")).thenReturn(List.of());

    handler.handle(command());

    verifyNoInteractions(client);
  }
}
