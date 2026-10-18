package io.github.opendonationassistant.webhook.listener.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.opendonationassistant.webhook.listener.handler.WebhookChangedEventHandler.WebhookChangedEvent;
import io.github.opendonationassistant.webhook.listener.handler.WebhookChangedEventHandler.WebhookChangedEvent.Event;
import io.github.opendonationassistant.webhook.listener.handler.WebhookChangedEventHandler.WebhookChangedEvent.Type;
import io.github.opendonationassistant.webhook.repository.Webhook;
import io.github.opendonationassistant.webhook.repository.WebhookDataRepository;
import io.micronaut.serde.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WebhookChangedEventHandlerTest {

  private WebhookDataRepository repository;
  private WebhookChangedEventHandler handler;

  @BeforeEach
  public void setUp() {
    repository = mock(WebhookDataRepository.class);
    handler = new WebhookChangedEventHandler(
      mock(ObjectMapper.class),
      repository
    );
  }

  private static WebhookChangedEvent event(Event event) {
    return event(Type.DISCORD, event);
  }

  private static WebhookChangedEvent event(Type type, Event event) {
    return new WebhookChangedEvent(
      "hook-1",
      "recipient-1",
      "https://example.com/hook",
      type,
      true,
      event
    );
  }

  @Test
  public void savesOnCreated() throws IOException {
    handler.handle(event(Event.WEBHOOK_CREATED));

    verify(repository).save(
      new Webhook("hook-1", "recipient-1", "https://example.com/hook", true)
    );
  }

  @Test
  public void savesOnUpdated() throws IOException {
    handler.handle(event(Event.WEBHOOK_UPDATED));

    verify(repository).save(
      new Webhook("hook-1", "recipient-1", "https://example.com/hook", true)
    );
  }

  @Test
  public void deletesOnDeleted() throws IOException {
    handler.handle(event(Event.WEBHOOK_DELETED));

    verify(repository).deleteById("hook-1");
    verify(repository, never()).save(any());
  }

  @Test
  public void skipsNonDiscordWebhooks() throws IOException {
    handler.handle(event(Type.TELEGRAM, Event.WEBHOOK_CREATED));

    verify(repository, never()).save(any());
    verify(repository, never()).deleteById(any());
  }
}
