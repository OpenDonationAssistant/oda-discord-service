package io.github.opendonationassistant.discord.listener.handler;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.integration.discord.DiscordClient;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

@Singleton
public class SendDiscordMessageHandler
  extends AbstractMessageHandler<
    SendDiscordMessageHandler.SendDiscordMessageCommand
  > {

  private final ODALogger log = new ODALogger(this);
  private final DiscordClient discord;

  @Inject
  public SendDiscordMessageHandler(ObjectMapper mapper, DiscordClient discord) {
    super(mapper);
    this.discord = discord;
  }

  @Override
  public void handle(SendDiscordMessageCommand message) throws IOException {
    try {
      var response = discord
        .sendMessage(
          message.recipientId(),
          message.refreshTokenId(),
          message.channelId(),
          message.content(),
          Boolean.TRUE.equals(message.tts())
        )
        .join();
      log.info(
        "Discord message sent",
        Map.of(
          "recipientId",
          message.recipientId(),
          "channelId",
          message.channelId(),
          "messageId",
          response.id()
        )
      );
    } catch (Exception e) {
      log.error(
        "Failed to send Discord message",
        Map.of(
          "recipientId",
          message.recipientId(),
          "channelId",
          message.channelId(),
          "error",
          Optional.ofNullable(e.getMessage()).orElse("")
        )
      );
    }
  }

  @Serdeable
  public static record SendDiscordMessageCommand(
    String recipientId,
    String refreshTokenId,
    String channelId,
    String content,
    @Nullable Boolean tts
  ) {}
}
