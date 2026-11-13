# AGENTS.md — Animemizer Bot

Telegram bot for anime discovery. Shikimori is the data source; Kinopoisk API Unofficial, AniLiberty, and OpenRouter resolve a single watch link per anime.

## Build, test, run

```powershell
mvn test                       # H2 + test profile, no env vars needed
mvn test -Dtest=ClassName      # single class; nested: -Dtest=Outer$Inner
mvn -q -DskipTests compile     # fast compile check
mvn clean package              # jar in target/
java -jar target/animemizer-bot-*.jar --spring.profiles.active=prod
```

- `mvnw` / `mvnw.cmd` exist; on Windows use `.\mvnw.cmd` if `mvn` is unavailable.
- No lint, formatter, codegen, or CI config exists. `mvn compile` + `mvn test` are the validation baseline.
- `application.yaml` sets `spring.profiles.active: dev`, so a plain `spring-boot:run` / jar start uses long polling.
- The IDE run config loads `.env`; running via Maven does not, so export env vars first.
- `local_scripts/deploy_to_micro_2.sh` builds and deploys to a hardcoded remote host. Do not run it unless explicitly asked.

## Required environment variables

```
TELEGRAM_BOT_TOKEN, TELEGRAM_BOT_USERNAME, TELEGRAM_BOT_API_SECRET_TOKEN
DATASOURCE_URL, DATASOURCE_USERNAME, DATASOURCE_PASSWORD
KINOPOISK_API_KEY, OPENROUTER_API_KEY
TELEGRAM_BOT_WEBHOOK_PATH, TELEGRAM_BOT_WEBHOOK_BASE_URL   # prod/webhook only
```

`TelegramBotProperties`, `KinopoiskProperties`, and `OpenRouterProperties` use `@Validated` + `@NotBlank`, so missing values fail startup. Tests use dummy values from `application-test.yaml`.

## Testing quirks

- `ShikimoriConnectorIntegrationTest` is **not** disabled and calls the live Shikimori API (search "Naruto"). `mvn test` therefore needs network and can be slow/flaky; skip with `-Dtest='!ShikimoriConnectorIntegrationTest'` when offline.
- `RealSiteSessionTest` is `@Disabled`; `KinopoiskGraphQLIntegrationTest` runs only when `KINOPOISK_API_KEY` is set.
- `@SpringBootTest` tests import `TestTelegramConfig` to replace `TelegramClient` with a Mockito mock.
- Schema changes require updating `src/test/resources/schema.sql` manually (tables: `anime`, `telegram_user`, `watchlist`). There is no Flyway/Liquibase; production schemas are managed outside the app.
- JSON fixtures live in `src/test/resources/json/shikimori/`.

## Update routing (`BotUpdateProcessor.java`)

```
Update → callback query?  → CallbackHandler chain (first canHandle wins)
       → text message?    → TextHandler chain first, then CommandHandler map
       → otherwise        → "unsupported" reply
```

- **TextHandlers run before CommandHandlers.** `SearchTextHandler` intentionally accepts all non-command text and `/search`; it must return `false` for every other command or it swallows them.
- Handlers return `PartialBotApiMethod<?>`. Returning `null` means the handler sends/edits Telegram messages itself; the processor skips `execute()`.
- Callback data is `PREFIX:payload` from `AnimemizerBotCallback` (`NEXT_EPISODE:123`, `SET_WATCH_STATUS:123:WATCHING`). Watch-link callbacks are `providerCode:publicId`. Prefixes must be unique because routing is first-match.
- Handlers are `@Component` beans auto-collected via `List<>` injection.

## Patterns and integrations

- **Placeholder-then-replace**: slow flows send a placeholder immediately, do async work on `applicationTaskExecutor`, then edit it (`EditMessageText` / `EditMessageCaption` / `EditMessageMedia`); if the placeholder send fails, return the result directly as a fallback.
- **Anime links in text**: list messages embed deep links `https://t.me/<bot>?start=show_<shikimoriId>` (text links cannot fire callbacks). `StartCommandHandler` parses the `show_` payload and opens the card via `AnimeCardSender`; the `SHOW_ANIME` callback remains only for legacy buttons.
- **Watch-link enrichment**: `AnimeService.enrichAndSaveIfRequired()` runs on every anime fetch, resolves one URL via `WatchLinkResolver`, and persists it. Failures are cached 24h in `providerFailures` to avoid repeat lookups.
- `KinopoiskWatchLinkResolver` searches Kinopoisk first (rewrites `kinopoisk.ru` → `kinokino.red`), then AniLiberty; OpenRouter only *selects* among returned candidates and must never invent IDs or URLs. AniLiberty results carry the Shikimori ID as a matching signal.
- Active integrations use `@HttpExchange` clients, not HTML scraping. Jsoup remains only in a manual Kinopoisk GraphQL test.
- New `@HttpExchange` clients must be registered in `HttpClientConfig` with a dedicated `RestClient` bean and `HttpServiceProxyFactory`.
- `WebConfig` registers a custom `AbstractHttpMessageConverter` routing `BotApiMethod`/`Update` through `Json2Service` (Jackson 2.x). Do not replace it with the default converter.

## Domain model (Spring Data JDBC, not JPA)

- All entities/DTOs are records with `@Table` / `@MappedCollection`.
- `Anime` is immutable; use `withId()`, `withDescription()`, `withWatchUrl()`.
- `AnimeSource.SHIKIMORI` prefixes public IDs with `shikimori_`; use `createPublicId()` / `extractIdFromPublicId()`.
- `WatchStatus` owns transition logic (`onAllEpisodesWatched()`, `isTerminal()`). `REWATCHING` was removed; legacy `SET_WATCH_STATUS:<id>:REWATCHING` callbacks are ignored gracefully.
- Caches are declared in `CacheConfig` (4 total): `animeById` 24h, `animeByTitle` 3h, `providerFailures` 24h, `animeRefreshCooldown` 12h. Title cache keys are lowercase for case-insensitive lookup. The refresh button (`RefreshCallbackHandler` → `AnimeService.refreshById`) rate-limits live Shikimori re-fetches per anime via `animeRefreshCooldown`; while active, taps do nothing.

## Code conventions

- Constructor injection only — no field `@Autowired`.
- No `var`; use explicit local variable types.
- No `catch (Exception)`; catch specific types and return `Optional.empty()` on lookup failures.
- SLF4J via `LoggerFactory` — no Lombok.
- Async: `CompletableFuture` with `@Qualifier("applicationTaskExecutor") TaskExecutor` (virtual threads).
- Partition rows with `Gatherers.windowFixed(n)` (e.g., status buttons in rows of 3).
- Build Telegram text/keyboards via `bot/message` (`AnimeMessageBuilder`, `TelegramMessageUtils`) instead of ad-hoc strings; escape Markdown with `TelegramMessageUtils`.
- Tests: JUnit 6 + AssertJ, `// Given` / `// When` / `// Then`, naming `should_[Expected]_when_[Condition]`.

## Other instruction sources

- `.github/copilot-instructions.md` — condensed build/architecture notes.
- `.github/instructions/spring-boot-conventions.instructions.md` — Copilot rules for `src/**`.
- `README.md` — features, commands, deployment details.
- `PROXYAI.md` — older assistant prompt; use only if it does not conflict with this file.
