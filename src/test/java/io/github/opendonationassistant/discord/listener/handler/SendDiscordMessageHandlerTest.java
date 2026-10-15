package io.github.opendonationassistant.discord.listener.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.integration.discord.DiscordApi.DiscordMessageResponse;
import io.github.opendonationassistant.integration.discord.DiscordClient;
import io.micronaut.serde.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SendDiscordMessageHandlerTest {

  private DiscordClient discord;
  private SendDiscordMessageHandler handler;

  @BeforeEach
  public void setUp() {
    discord = mock(DiscordClient.class);
    handler = new SendDiscordMessageHandler(mock(ObjectMapper.class), discord);
  }

  private static SendDiscordMessageHandler.SendDiscordMessageCommand command() {
    return new SendDiscordMessageHandler.SendDiscordMessageCommand(
      "recipient",
      "refresh-token",
      "channel",
      "hello",
      Boolean.FALSE
    );
  }

  @Test
  public void sendsMessage() {
    when(
      discord.sendMessage(
        eq("recipient"),
        eq("refresh-token"),
        eq("channel"),
        eq("hello"),
        anyBoolean()
      )
    ).thenReturn(
      CompletableFuture.completedFuture(
        new DiscordMessageResponse("m-1", "channel", "hello")
      )
    );

    assertDoesNotThrow(() -> handler.handle(command()));

    verify(discord).sendMessage(
      "recipient",
      "refresh-token",
      "channel",
      "hello",
      false
    );
  }

  @Test
  public void treatsMissingTtsAsFalse() {
    when(
      discord.sendMessage(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        anyBoolean()
      )
    ).thenReturn(
      CompletableFuture.completedFuture(
        new DiscordMessageResponse("m-1", "channel", "hello")
      )
    );

    assertDoesNotThrow(() ->
      handler.handle(
        new SendDiscordMessageHandler.SendDiscordMessageCommand(
          "recipient",
          "refresh-token",
          "channel",
          "hello",
          null
        )
      )
    );

    verify(discord).sendMessage(
      "recipient",
      "refresh-token",
      "channel",
      "hello",
      false
    );
  }

  @Test
  public void swallowsSendFailure() {
    when(
      discord.sendMessage(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        anyBoolean()
      )
    ).thenThrow(new RuntimeException("boom"));

    assertDoesNotThrow(() -> handler.handle(command()));
  }
}
