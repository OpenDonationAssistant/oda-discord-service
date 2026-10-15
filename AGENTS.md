# PROJECT KNOWLEDGE BASE

**Generated:** 2026-09-18

## OVERVIEW
Micronaut 5.1 / Java 25 microservice in the OpenDonationAssistant ecosystem. Consumes
`SendDiscordMessageCommand` from RabbitMQ and posts the message to a Discord channel.
OAuth token is obtained from `oda-recipient-service` via RabbitMQ RPC. Ships as a
GraalVM native image. Stateless: no tables, all data arrives in the command.

## STRUCTURE
```
oda-discord-service/
├── src/main/java/io/github/opendonationassistant/
│   ├── Application.java              # main() + RabbitMQ @Factory + env pinning
│   ├── discord/listener/             # RabbitMQ consumer
│   │   └── handler/                  # typed command handler
│   └── integration/discord/          # Discord HTTP client + TokenRPC facade
├── src/main/resources/
│   ├── application*.yml              # 3 env profiles (standalone forced)
│   └── logback.xml
├── src/test/java/                    # JUnit 5 + Mockito unit tests
├── pom.xml                           # Micronaut BOM, ErrorProne + NullAway gates
├── aot-{jar,native-image}.properties
├── Dockerfile                        # copies native binary, fedora:44
└── .github/workflows/maven.yml       # delegates to oda-libraries release workflow
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Add a command handler | `discord/listener/handler/` | extend `AbstractMessageHandler<T>` |
| Change RabbitMQ wiring | `Application.java` | `@Factory` declares exchanges/queues |
| Call the Discord API | `integration/discord/DiscordClient.java` | token handling lives here |
| Add a Discord endpoint | `integration/discord/DiscordApi.java` | declarative `@Client("discord")` |

## CODE MAP
| Symbol | Type | Location | Role |
|--------|------|----------|------|
| `Application` | class + `@Factory` | root | Bootstrap, RabbitMQ wiring, pins `standalone` env |
| `CommandListener` | `@RabbitListener` | discord/listener | Consumes `discord.command`, delegates to `MessageProcessor` |
| `SendDiscordMessageHandler` | `@Singleton` | discord/listener/handler | Handles `SendDiscordMessageCommand` |
| `DiscordClient` | `@Singleton` | integration/discord | Fetches token via `TokenRPC`, calls Discord |
| `DiscordApi` | `@Client("discord")` | integration/discord | `POST /api/v10/channels/{channelId}/messages` |

## CONVENTIONS
- **Constructor injection only** (`@Inject`), no field injection.
- **DTOs are Java records** annotated `@Serdeable`; nullable fields marked `@Nullable` (JSpecify).
- **NullAway is ERROR** on `io.github.opendonationassistant` (JSpecify mode). Root package is `@NullUnmarked`.
- **Handlers** extend `AbstractMessageHandler<T>` from oda-rabbit-conf; the generic payload's
  simple name must equal the RabbitMQ `type` header (`SendDiscordMessageCommand`).
- **All outbound Discord calls go through `DiscordClient`** (never `DiscordApi` from handlers).
- **Tests**: plain JUnit 5 + Mockito unit tests (no `@MicronautTest`).
- **Git commits**: lowercase, short imperative.

## ANTI-PATTERNS
- **Do NOT add field injection** — constructor injection only.
- **Do NOT write null-unsafe code** — NullAway fails the build; annotate `@Nullable`.
- **Do NOT add a RabbitMQ exchange/queue in a listener** — declare it in `Application`'s `@Factory`.
- **Do NOT call the Discord API directly from handlers** — go through `DiscordClient`.
- **Do NOT add `@MicronautTest`/Testcontainers tests** — existing tests are context-free.
- **Do NOT commit** `target/`, `.mvn/wrapper/`, `.tmp/`, `.micronaut/`, IDE files — gitignored.

## COMMANDS
```bash
mvn test                          # unit tests + JaCoCo (NullAway gate)
mvn verify                        # tests + coverage report
mvn clean package -Dpackaging=native-image -DskipTests   # CI native build
mvn clean package -Dpackaging=jar # JVM jar build
```

## NOTES
- `MessageProcessor.process` is `@Transactional`, so a datasource (Postgres) is configured even
  though the service persists nothing. Flyway schema `discord`.
- `@Client("discord")` base URL comes from `micronaut.http.services.discord.url`
  (`DISCORD_API_URL` env var), set at deploy time.
- Token RPC: `TokenRPC.TokenRequest(recipientId, refreshTokenId)` → `TokenResponse`.
  The recipient service only returns a token if the refresh token belongs to the recipient.
- Known caveat: Discord's `Create Message` requires a bot token (`Authorization: Bot ...`).
  This service mirrors Twitch and sends `Bearer <token>` using the token held by
  `oda-recipient-service`, which currently stores user OAuth2 tokens.
- Shared logic lives in `oda-rabbit-conf` (0.11.232): `AMQPConfiguration`, `MessageProcessor`,
  `AbstractMessageHandler`, `TokenRPC`.
