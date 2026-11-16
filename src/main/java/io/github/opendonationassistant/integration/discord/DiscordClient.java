package io.github.opendonationassistant.integration.discord;

import io.github.opendonationassistant.commons.problem.ApiProblem;
import io.github.opendonationassistant.integration.discord.DiscordApi.DiscordMessageRequest;
import io.github.opendonationassistant.integration.discord.DiscordApi.DiscordMessageResponse;
import io.github.opendonationassistant.rabbit.AsyncTokenRPC;
import io.github.opendonationassistant.rabbit.TokenRPC.TokenRequest;
import io.github.opendonationassistant.rabbit.TokenRPC.TokenResponse;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.zalando.problem.Status;

@Singleton
public class DiscordClient {

  private final DiscordApi api;
  private final AsyncTokenRPC asyncTokenRpc;

  @Inject
  public DiscordClient(DiscordApi api, AsyncTokenRPC asyncTokenRpc) {
    this.api = api;
    this.asyncTokenRpc = asyncTokenRpc;
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
    return asyncTokenRpc
      .token(new TokenRequest(recipientId, refreshTokenId))
      .thenCompose(token -> {
        if (token == null || token.token() == null) {
          return CompletableFuture.failedFuture(
            ApiProblem.of(
              Status.UNAUTHORIZED,
              "upstream_unauthorized",
              "Unauthorized",
              Optional.ofNullable(token)
                .map(TokenResponse::message)
                .orElse("Unauthorized")
            )
          );
        }
        var authHeader = "Bearer %s".formatted(token.token());
        return fn.apply(authHeader);
      });
  }
}
