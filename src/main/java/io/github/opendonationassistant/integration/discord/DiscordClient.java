package io.github.opendonationassistant.integration.discord;

import io.github.opendonationassistant.integration.discord.DiscordApi.DiscordMessageRequest;
import io.github.opendonationassistant.integration.discord.DiscordApi.DiscordMessageResponse;
import io.github.opendonationassistant.rabbit.TokenRPC;
import io.github.opendonationassistant.rabbit.TokenRPC.TokenRequest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.zalando.problem.Problem;

@Singleton
public class DiscordClient {

  private final DiscordApi api;
  private final TokenRPC tokenRPC;

  @Inject
  public DiscordClient(DiscordApi api, TokenRPC tokenRPC) {
    this.api = api;
    this.tokenRPC = tokenRPC;
  }

  public CompletableFuture<DiscordMessageResponse> sendMessage(
    String recipientId,
    String refreshTokenId,
    String channelId,
    String content,
    boolean tts
  ) {
    return runWithToken(recipientId, refreshTokenId, auth ->
      api.sendMessage(auth, channelId, new DiscordMessageRequest(content, tts))
    );
  }

  private <T> CompletableFuture<T> runWithToken(
    String recipientId,
    String refreshTokenId,
    Function<String, CompletableFuture<T>> fn
  ) {
    return CompletableFuture.supplyAsync(() ->
      tokenRPC.token(new TokenRequest(recipientId, refreshTokenId))
    ).thenCompose(token -> {
      if (token == null || token.token() == null) {
        return CompletableFuture.failedFuture(
          Problem.builder()
            .withTitle("Unauthorized")
            .withDetail(
              Optional.ofNullable(token.message()).orElse("Unauthorized")
            )
            .build()
        );
      }
      var authHeader = "Bearer %s".formatted(token.token());
      return fn.apply(authHeader);
    });
  }
}
