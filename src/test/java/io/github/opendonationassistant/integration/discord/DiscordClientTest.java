package io.github.opendonationassistant.integration.discord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.integration.discord.DiscordApi.DiscordMessageRequest;
import io.github.opendonationassistant.integration.discord.DiscordApi.DiscordMessageResponse;
import io.github.opendonationassistant.rabbit.AsyncTokenRPC;
import io.github.opendonationassistant.rabbit.TokenRPC.TokenRequest;
import io.github.opendonationassistant.rabbit.TokenRPC.TokenResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

public class DiscordClientTest {

  private DiscordApi api;
  private AsyncTokenRPC asyncTokenRpc;
  private DiscordClient client;

  @BeforeEach
  public void setUp() {
    api = mock(DiscordApi.class);
    asyncTokenRpc = mock(AsyncTokenRPC.class);
    client = new DiscordClient(api, asyncTokenRpc);
  }

  private static CompletableFuture<DiscordMessageResponse> sendMessage(
    DiscordClient client
  ) {
    return client.sendMessage(
      "recipient",
      "refresh-token",
      "channel",
      "hello",
      false
    );
  }

  private static CompletableFuture<TokenResponse> token(TokenResponse token) {
    return CompletableFuture.completedFuture(token);
  }

  @Test
  public void sendsMessageWithResolvedToken() {
    var request = new DiscordMessageRequest("hello", false);
    var response = new DiscordMessageResponse("m-1", "channel", "hello");
    when(asyncTokenRpc.token(new TokenRequest("recipient", "refresh-token")))
      .thenReturn(token(new TokenResponse("token", "fine")));
    when(api.sendMessage("Bearer token", "channel", request)).thenReturn(
      CompletableFuture.completedFuture(response)
    );

    var result = sendMessage(client);

    assertSame(response, result.join());
    verify(api).sendMessage("Bearer token", "channel", request);
  }

  @Test
  public void failsWithUnauthorizedWhenTokenIsMissing() {
    when(asyncTokenRpc.token(any(TokenRequest.class))).thenReturn(
      token(new TokenResponse(null, "no token"))
    );

    var result = sendMessage(client);

    var error = assertThrows(CompletionException.class, () -> result.join());
    var problem = assertInstanceOf(Problem.class, error.getCause());
    assertEquals("Unauthorized", problem.getTitle());
    assertEquals("no token", problem.getDetail());
    verifyNoInteractions(api);
  }

  @Test
  public void failsWithUnauthorizedWhenMessageIsMissing() {
    var token = mock(TokenResponse.class);
    doReturn(null).when(token).message();
    when(asyncTokenRpc.token(any(TokenRequest.class))).thenReturn(
      CompletableFuture.completedFuture(token)
    );

    var result = sendMessage(client);

    var error = assertThrows(CompletionException.class, () -> result.join());
    var problem = assertInstanceOf(Problem.class, error.getCause());
    assertEquals("Unauthorized", problem.getTitle());
    assertEquals("Unauthorized", problem.getDetail());
    verifyNoInteractions(api);
  }

  @Test
  public void failsWithUnauthorizedWhenTheRpcReturnsNoResponse() {
    when(asyncTokenRpc.token(any(TokenRequest.class))).thenReturn(
      CompletableFuture.completedFuture(null)
    );

    var result = sendMessage(client);

    var error = assertThrows(CompletionException.class, () -> result.join());
    var problem = assertInstanceOf(Problem.class, error.getCause());
    assertEquals("Unauthorized", problem.getTitle());
    assertEquals("Unauthorized", problem.getDetail());
    verifyNoInteractions(api);
  }

  @Test
  public void propagatesTokenFailure() {
    when(asyncTokenRpc.token(any(TokenRequest.class))).thenReturn(
      CompletableFuture.failedFuture(new IllegalStateException("boom"))
    );

    var result = sendMessage(client);

    var error = assertThrows(CompletionException.class, () -> result.join());
    assertInstanceOf(IllegalStateException.class, error.getCause());
    verifyNoInteractions(api);
  }
}
