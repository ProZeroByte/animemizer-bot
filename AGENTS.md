# AGENTS.md — Animemizer Bot

Telegram bot for anime discovery. Integrates Shikimori (primary), Anilibria, AnimeGo, YummyAnime.

## Tech Stack (exact versions)

- **Java 26** — virtual threads enabled by default, Records, pattern matching, Stream Gatherers
- **Spring Boot 4.1.1** / Spring Framework 7.x (version managed by Spring Boot parent)
- **Jetty** (not Tomcat) — Tomcat is excluded in pom.xml
- **Jackson 3.x + 2.x** (dual) — both `tools.jackson` and `com.fasterxml.jackson` mappers configured in `JacksonConfig.java`; version managed by Spring Boot parent
- **PostgreSQL** (prod) / **H2** (test) via `spring-boot-starter-data-jdbc` (not JPA)
- **Caffeine** cache — 4 named caches configured in `CacheConfig.java`
- **telegrambots 10.3.0** — `telegrambots-springboot-longpolling-starter` + `telegrambots-client-jetty-adapter`
- **Jsoup** for HTML scraping (AnimeGo, YummyAnime providers)

## Build & Run

```powershell
mvn clean package                                   # build
mvn test                                            # run tests (uses H2 + application-test.yaml)
java -jar target/animemizer-bot-*.jar --spring.profiles.active=dev   # long polling
java -jar target/animemizer-bot-*.jar --spring.profiles.active=prod  # webhook
```

Docker: `docker build -t animemizer-bot:latest . && docker-compose up -d`

No single-test shortcut configured — standard `mvn test -Dtest=ClassName` works.

## Required Environment Variables

```
TELEGRAM_BOT_TOKEN, TELEGRAM_BOT_USERNAME, TELEGRAM_BOT_API_SECRET_TOKEN
DATASOURCE_URL, DATASOURCE_USERNAME, DATASOURCE_PASSWORD
TELEGRAM_BOT_WEBHOOK_PATH, TELEGRAM_BOT_WEBHOOK_BASE_URL (prod only)
```

Tests use dummy values in `application-test.yaml` — no env vars needed for `mvn test`.

## Architecture — What's Non-Obvious

### Update Routing (`BotUpdateProcessor.java`)

```
Update → switch (pattern matching on Update type)
  hasCallbackQuery?      → CallbackHandler chain (first-match by canHandle)
  hasMessage+hasText?    → TextHandler chain first, then CommandHandler map fallback
  else                   → "unsupported" reply
```

**TextHandlers are checked BEFORE CommandHandlers.** `SearchTextHandler` catches all text including `/search`, so it must return `false` for other commands or they'll be swallowed.

Handler return type is `PartialBotApiMethod<?>`. Returning `null` means "I'm managing my own Telegram responses" (placeholder-then-replace pattern). The processor skips `execute()` for null returns.

### Command Routing

- `CommandHandler` — matched by exact command name from `AnimemizerBotCommand.getCommandName()`
- `CallbackHandler` — matched by `AnimemizerBotCallback` prefix (e.g., `NEXT_EPISODE:`)
- `TextHandler` — flexible predicate via `canHandle(String)`
- All handlers are `@Component` and auto-registered via Spring `List<>` injection

### Callback Data Format

All callbacks: `PREFIX:payload` managed by `AnimemizerBotCallback` enum.
- Simple: `NEXT_EPISODE:123`
- Composite: `SET_WATCH_STATUS:123:WATCHING`
- Watch link: `providerCode:publicId` (parsed by `extractWatchLinkPayload`)

When adding a new callback enum value, the prefix must be unique — routing iterates all handlers, first match wins.

### Placeholder-then-Replace Pattern

For slow operations (search, navigation, list):
1. Send placeholder message immediately
2. Do async work
3. Edit placeholder in-place (`EditMessageText` / `EditMessageCaption` / `EditMessageMedia`)
4. If placeholder send fails, return fallback result directly

### Multi-Provider Enrichment

`AnimeService.enrichAndSaveIfRequired()` is called on every anime fetch. It checks all `AnimeWatchProvider` implementations for missing links and fills them. Failed lookups are cached 24h in `providerCheckFailures` to avoid re-scraping.

**Providers:**
- **Shikimori**: `@HttpExchange` client → `ShikimoriMapper` → full `Anime` domain objects (primary source)
- **Anilibria**: `@HttpExchange` client → external ID only
- **AnimeGo**: Jsoup scraping → external ID only
- **YummyAnime**: Jsoup + DLE session/cookie management via `YummyAnimeDleSessionManager` → external ID only

New `@HttpExchange` clients must be registered in `HttpClientConfig` with a dedicated `RestClient` bean and `HttpServiceProxyFactory`. Scraping providers just use Jsoup directly with `NetworkProperties.getRandomAgent()`.

### Domain Model Quirks

- All entities are **Records** with `@Table`/`@MappedCollection` (Spring Data JDBC, not JPA)
- `Anime` has `withId()`, `withDescription()`, `withLinks()` copy methods because Records are immutable
- `AnimeSource` prefixes: `shikimori_`, `anilibria_`, `animego_`, `yummyanime_` — use `createPublicId()` / `extractIdFromPublicId()`
- `WatchStatus` has transition logic: `onAllEpisodesWatched()`, `onRewatchStarted()`, `isTerminal()`

### HTTP Client Setup

`HttpClientConfig` manually builds `RestClient` → `RestClientAdapter` → `HttpServiceProxyFactory` → client proxy for each `@HttpExchange` interface. Spring's declarative HTTP client pattern, not Feign.

### WebConfig — Custom Telegram JSON Converter

`WebConfig` registers a custom `AbstractHttpMessageConverter` that routes `BotApiMethod` and `Update` serialization through `Json2Service` (which uses Jackson 2.x `ObjectMapper`). This is needed because the telegrambots library expects specific JSON handling.

## Code Conventions

- **Constructor injection only** — no `@Autowired` on fields
- **Records for all DTOs** and domain entities
- **No `var`** — always explicit type names for local variables
- **No `catch (Exception)`** — catch specific types, return `Optional.empty()` on failures
- **SLF4J with `LoggerFactory`** — no Lombok `@Slf4j`
- **Async**: `CompletableFuture` with `@Qualifier("applicationTaskExecutor") TaskExecutor`
- **Stream Gatherers**: `Gatherers.windowFixed(n)` for partitioning (e.g., status buttons in rows of 3, watch link buttons in rows of 2)

## Testing

- **JUnit 6 + AssertJ**, BDD style: `// Given` / `// When` / `// Then`
- **Naming**: `should_[Expected]_when_[Condition]`
- **Test profile**: `application-test.yaml` → H2 in-memory, cache disabled, dummy Telegram tokens
- **Schema**: `src/test/resources/schema.sql` auto-loaded — contains all 4 tables (`anime`, `anime_link`, `telegram_user`, `watchlist`). Update when adding new entities or columns.
- Test JSON fixtures in `src/test/resources/json/shikimori/`

## Configuration

**Profiles:**
- `dev` — long polling (`TelegramBotConfig` `@Profile("dev")` bean), DEBUG logging
- `prod` — webhook (`WebhookController` + `TelegramBotConfig.setWebhook()` bean + `TelegramBotService.initialize()`), INFO logging
- `test` — H2, disabled cache, dummy tokens

**App config keys (`app.*`):**
- `app.anime-sources` — `Map<AnimeSource, String>` of provider base URLs
- `app.watch-url-template` — redirect URL template (`%s` = source name, `%s` = publicId)
- `app.network.provider-timeout-ms` — HTTP timeout for scraping (default 5000ms)
- `app.network.user-agents` — UA rotation list for `NetworkProperties.getRandomAgent()`

`TelegramBotProperties` uses `@Validated` + `@NotBlank` — missing token/username fails fast at startup.

## Deployment

Docker: Alpine JRE 26, 416MB memory limit, `-Xmx256m -Xms256m`, timezone `Europe/Warsaw`.
Internal port 8080 exposed via `external-net` Docker network (reverse proxy not included in docker-compose.yml).

## Existing Instruction Sources

- `.github/instructions/spring-boot-conventions.instructions.md` — Copilot instructions covering same conventions (apply to `src/**`)
- `.mimocode/instructions.md` — Mimo AI project context (derived from AGENTS.md + conventions)
- `PROXYAI.md` — older assistant prompt with testing and coding standards
