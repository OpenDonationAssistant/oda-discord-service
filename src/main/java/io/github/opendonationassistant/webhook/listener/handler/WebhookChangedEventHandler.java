package io.github.opendonationassistant.webhook.listener.handler;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.events.HasRecipientId;
import io.github.opendonationassistant.webhook.repository.Webhook;
import io.github.opendonationassistant.webhook.repository.WebhookDataRepository;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.Map;

@Singleton
public class WebhookChangedEventHandler
  extends AbstractMessageHandler<
    WebhookChangedEventHandler.WebhookChangedEvent
  > {

  private final ODALogger log = new ODALogger(this);
  private final WebhookDataRepository repository;

  @Inject
  public WebhookChangedEventHandler(
    ObjectMapper mapper,
    WebhookDataRepository repository
  ) {
    super(mapper);
    this.repository = repository;
  }

  @Override
  public void handle(WebhookChangedEvent message) throws IOException {
    if (message.type() != WebhookChangedEvent.Type.DISCORD) {
      log.debug(
        "Skipping non-Discord webhook",
        Map.of("id", message.id(), "type", message.type())
      );
      return;
    }
    log.info(
      "Syncing webhook",
      Map.of(
        "id",
        message.id(),
        "recipientId",
        message.recipientId(),
        "event",
        message.event()
      )
    );
    switch (message.event()) {
      case WEBHOOK_CREATED, WEBHOOK_UPDATED -> repository.save(
        new Webhook(
          message.id(),
          message.recipientId(),
          message.url(),
          message.enabled()
        )
      );
      case WEBHOOK_DELETED -> repository.deleteById(message.id());
    }
  }

  @Serdeable
  public static record WebhookChangedEvent(
    String id,
    String recipientId,
    String url,
    Type type,
    boolean enabled,
    Event event
  )
    implements HasRecipientId {
    @Serdeable
    public static enum Type {
      DISCORD,
      TELEGRAM,
    }

    @Serdeable
    public static enum Event {
      WEBHOOK_CREATED,
      WEBHOOK_UPDATED,
      WEBHOOK_DELETED,
    }
  }
}
