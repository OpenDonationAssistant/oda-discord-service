# ODA Discord Service
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/OpenDonationAssistant/oda-discord-service)

Micronaut service that consumes Discord commands from RabbitMQ and publishes messages to
Discord channels. The OAuth token used for the Discord API is obtained from
`oda-recipient-service` over RabbitMQ RPC (`TokenRPC`, queue `recipient.token-request`).

## How it works

1. A command `SendDiscordMessageCommand` is published to the `commands` exchange with
   routing key `command.SendDiscordMessageCommand`.
2. `CommandListener` consumes it from the `discord.command` queue and routes it through
   the shared `MessageProcessor`.
3. `SendDiscordMessageHandler` calls `DiscordClient`, which fetches an access token from
   `oda-recipient-service` (by `recipientId` + `refreshTokenId`) and performs
   `POST /api/v10/channels/{channelId}/messages` with `Authorization: Bearer <token>`.

### Command payload

```json
{
  "recipientId": "...",
  "refreshTokenId": "...",
  "channelId": "...",
  "content": "Hello from ODA",
  "tts": false
}
```

`tts` is optional and defaults to `false`.

## Running with Docker

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `RABBITMQ_HOST` | RabbitMQ host | `localhost` |
| `JWKS_URI` | Keycloak JWKS URL | (required in prod) |
| `JDBC_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost/postgres` |
| `JDBC_USER` | PostgreSQL username | `postgres` |
| `JDBC_PASSWORD` | PostgreSQL password | `postgres` |
| `DISCORD_API_URL` | Discord API base URL | `https://discord.com` |

### Docker Run Example

```bash
docker run -d \
  --name oda-discord-service \
  -p 8080:8080 \
  -e RABBITMQ_HOST=rabbitmq \
  -e JDBC_URL=jdbc:postgresql://postgres:5432/postgres?currentSchema=discord \
  -e JDBC_USER=postgres \
  -e JDBC_PASSWORD=your_password \
  -e JWKS_URI=https://auth.oda.digital/realms/ODA/protocol/openid-connect/certs \
  ghcr.io/opendonationassistant/oda-discord-service:latest
```

## Build

```bash
mvn test                              # unit tests (NullAway gate)
mvn clean package -Dpackaging=jar     # JVM jar
mvn clean package -Dpackaging=native-image -DskipTests   # CI native build
```
