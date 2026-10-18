package io.github.opendonationassistant.webhook.listener.handler;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.integration.webhook.WebhookClient;
import io.github.opendonationassistant.webhook.repository.Webhook;
import io.github.opendonationassistant.webhook.repository.WebhookDataRepository;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.Map;

@Singleton
public class SendWebhookMessageHandler
  extends AbstractMessageHandler<
    SendWebhookMessageHandler.SendDiscordWebhookMessageCommand
  > {

  private final ODALogger log = new ODALogger(this);
  private final WebhookDataRepository repository;
  private final WebhookClient client;

  @Inject
  public SendWebhookMessageHandler(
    ObjectMapper mapper,
    WebhookDataRepository repository,
    WebhookClient client
  ) {
    super(mapper);
    this.repository = repository;
    this.client = client;
  }

  @Override
  public void handle(SendDiscordWebhookMessageCommand message) throws IOException {
    var webhooks = repository.findByRecipientId(message.recipientId());
    log.info(
      "Dispatching message to webhooks",
      Map.of(
        "recipientId",
        message.recipientId(),
        "webhookCount",
        webhooks.size()
      )
    );
    webhooks
      .stream()
      .filter(Webhook::enabled)
      .forEach(webhook ->
        client.sendMessage(webhook.url(), message.message())
      );
  }

  @Serdeable
  public static record SendDiscordWebhookMessageCommand(
    String recipientId,
    String message
  ) {}
}
