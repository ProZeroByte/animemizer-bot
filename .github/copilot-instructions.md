# Copilot instructions for animemizer-bot

## Build, test, and validation

Use Maven from the repo root.

```powershell
./mvnw clean package
./mvnw test
./mvnw test -Dtest=pro.zerobyte.animemizerbot.bot.handler.command.StartCommandHandlerTest
./mvnw -q -DskipTests compile
```

Single-test execution is the standard pattern for this repo:

```powershell
./mvnw test -Dtest=ClassName
```

There is no dedicated lint task configured in `pom.xml`. Treat `mvn test` and `mvn compile` as the repo’s validation baseline.

## High-level architecture

This is a Spring Boot Telegram bot that searches anime via Shikimori and resolves a watch link via Tavily web search.

- `Application.java` boots the app and enables scheduling.
- `TelegramBotService` is the entry point for Telegram updates in both long-polling (`dev`) and webhook (`prod`) modes.
- `BotUpdateProcessor` is the routing layer: inline keyboard callbacks are checked first, then text messages, then command handlers.
- Handler types are split by responsibility:
  - `CommandHandler` for `/start`, `/help`, `/stop`, `/list`, `/batch`
  - `CallbackHandler` for inline keyboard actions like episode changes, watch-state updates, and related anime navigation
  - `TextHandler` for free-form anime search and `/search`
- `AnimeService` owns fetch/save logic and resolves a watch link before persisting anime data.
- `ShikimoriConnector` and `ShikimoriClient` are the primary data source; `TavilyWatchLinkResolver` resolves the watch link.
- `WatchlistService` and repository-backed `core/watchlist` model track per-user progress and statuses.
- Data access uses Spring Data JDBC (not JPA), with PostgreSQL in production and H2 in tests.

The non-obvious routing behavior matters: text handlers are evaluated before command handlers, and `SearchTextHandler` intentionally catches plain text and `/search`-style input. Returning `null` from a handler means “I’m managing my own Telegram response” and triggers the placeholder-then-replace flow.

## Key conventions and project-specific patterns

### Java and Spring conventions

- Use constructor injection only; no field `@Autowired`.
- Prefer records for DTOs and domain entities.
- Use explicit variable types; do not use `var`.
- Use SLF4J `LoggerFactory` and avoid Lombok `@Slf4j`.
- Catch specific exceptions only; avoid `catch (Exception)` and do not swallow failures without handling.
- Use `CompletableFuture` with the virtual-thread executor (`@Qualifier("applicationTaskExecutor")`) for async work.
- Keep callback payloads in the `PREFIX:payload` format defined by `AnimemizerBotCallback`.

### Telegram bot behavior

- The bot uses a placeholder-then-replace pattern for slower flows such as search and list rendering: send a temporary message, do work asynchronously, then edit it in place.
- `SearchTextHandler` is intentionally broad and must return `false` for other commands to avoid swallowing `/help`, `/list`, and similar inputs.
- Bot responses are built via the message builders and Telegram utilities in `bot/message` rather than ad hoc string formatting.

### Provider integration model

- Shikimori is the main source of anime data via `@HttpExchange` REST clients and mapper classes.
- Tavily is used to resolve a single watch link via `@HttpExchange` REST client configured in `HttpClientConfig`.
- Watch link resolution is centralized through the `WatchLinkResolver` interface; failed lookups are cached to avoid repeated searches.
- The watch link is stored on the `Anime` entity (`watchUrl`) and rendered as a single "Смотреть" button.

### Testing and configuration

- Tests run with the `application-test.yaml` profile and H2 in-memory DB.
- Test naming follows `should_[ExpectedBehavior]_when_[Condition]` with BDD comments (`// Given`, `// When`, `// Then`).
- Schema changes require updates to `src/test/resources/schema.sql`.
- `TelegramBotProperties` uses validation, so missing Telegram config fails fast at startup.
- Production uses webhook mode; development uses long polling.

### Repo-specific architectural cues

- The app uses Spring Data JDBC records with `@Table` and `@MappedCollection`, not JPA entities.
- Domain conventions like `AnimeSource` public ID generation/extraction and `WatchStatus` transition logic should be preserved when editing related code.
- Web configuration includes a custom Telegram JSON converter for `BotApiMethod`/`Update` serialization; do not rewrite this lightly.
- Cache keys for title-based lookups are expected to be lowercase for case-insensitive behavior.

## Files worth reading first when changing behavior

For a targeted change in bot behavior, start with the routing and service layer:

- `src/main/java/pro/zerobyte/animemizerbot/bot/processor/BotUpdateProcessor.java`
- `src/main/java/pro/zerobyte/animemizerbot/bot/handler/AnimemizerBotCallback.java`
- `src/main/java/pro/zerobyte/animemizerbot/core/anime/AnimeService.java`
- `src/main/java/pro/zerobyte/animemizerbot/config/HttpClientConfig.java`
- `src/main/java/pro/zerobyte/animemizerbot/integration/shikimori/ShikimoriConnector.java`

For a watch-link change, read the `integration/tavily` package and `TavilyProperties` next.

## Relevant project docs

- `README.md` — application overview, commands, architecture, and deployment notes
- `AGENTS.md` — detailed repo-specific bot and Spring conventions
- `.github/instructions/spring-boot-conventions.instructions.md` — coding rules applied to `src/**`
