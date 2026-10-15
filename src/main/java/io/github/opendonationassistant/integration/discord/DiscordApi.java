package io.github.opendonationassistant.integration.discord;

import static io.micronaut.http.HttpHeaders.AUTHORIZATION;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.serde.annotation.Serdeable;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

@Client("discord")
@Header(name = "User-Agent", value = "DiscordBot (https://oda.digital, 0.1.0)")
public interface DiscordApi {
  @Post("/api/v10/channels/{channelId}/messages")
  CompletableFuture<DiscordMessageResponse> sendMessage(
    @Header(AUTHORIZATION) String authorization,
    @PathVariable String channelId,
    @Body DiscordMessageRequest request
  );

  @Serdeable
  record DiscordMessageRequest(String content, Boolean tts) {}

  @Serdeable
  record DiscordMessageResponse(
    String id,
    @JsonProperty("channel_id") String channelId,
    @Nullable String content
  ) {}
}
