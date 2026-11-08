---
applyTo: 'src/**'
---

# Animemizer Bot — Spring Boot Conventions

## Project Context

This is a **Telegram bot** (Animemizer Bot) that helps users discover anime by integrating with Shikimori (anime data) and Tavily (watch link search). Built with **Spring Boot 4.0.6** and **Java 26** using virtual threads, Jetty server, and Maven.

## Tech Stack

- **Java 26** — use modern features: Records, pattern matching, sealed classes, virtual threads
- **Spring Boot 4.0.6** / Spring Framework 7.0.7
- **PostgreSQL** (prod) / **H2** (test)
- **Caffeine** cache with Spring Cache abstraction
- **telegrambots 9.6.0** (long-polling + webhook)
- **Jetty** (not Tomcat)
- **Jsoup** for HTML scraping (legacy — no longer used by active integrations)
- **JUnit 6** + AssertJ for testing

## Mandatory Coding Rules

### Dependency Injection

- **Never** use `@Autowired` on fields. Always use **constructor injection** with `final` fields.
- Prefer a single constructor (no need for `@Autowired` annotation on it).

```java
@Component
public class MyService {
    private final SomeDependency dep;

    public MyService(SomeDependency dep) {
        this.dep = dep;
    }
}
```

### DTOs Are Records

All data-transfer objects must be Java **Records** — immutable, concise, no Lombok:

```java
public record ShikimoriAnimeDto(
    Long id,
    String name,
    String russian,
    List<String> genres
) {}
```

### Error Handling

- **No** `catch (Exception)` or `catch (RuntimeException)` — always catch specific exception types.
- Return `Optional.empty()` on failures; log the specific exception.
- Use `@RestControllerAdvice` (`GlobalExceptionHandler`) for centralized handling in controllers.

### Logging

- Use SLF4J with `LoggerFactory` — **no** Lombok `@Slf4j`.
- Log levels: `ERROR` for failures, `WARN` for suspicious states, `DEBUG` for flow tracing.

### Async Patterns

- Use `CompletableFuture` with `@Qualifier("applicationTaskExecutor")` virtual thread executor.
- Use `@Cacheable` for expensive operations (searches, external API calls).
- Use `thenCombine()` for fan-out parallel requests.
- **Never** block in async handlers (`Thread.sleep()`, `.get()` without timeout).

### Variable Declarations

- **Do not** use `var` for local variables. Always use explicit type names for clarity:

```java
// ✅ Correct
Message message = update.getMessage();
Optional<Anime> anime = animeService.findById(animeId);

// ❌ Wrong
var message = update.getMessage();
var anime = animeService.findById(animeId);
```

### Cache Keys

- Cache keys for title-based lookups must be **lowercase** (case-insensitive queries).

## Architecture Patterns

### Update Processing Pipeline

```
Telegram Update → PollingBot / WebhookController
  → BotUpdateProcessor (routing)
    → TextHandler (plain text / /search) | CallbackHandler (button clicks) | CommandHandler (/start, /help, etc.)
      → Business logic (service layer)
        → Telegram API response (sendMessage, editMessage, etc.)
```

### Handler Types

| Interface | Purpose | Routing | Example |
|-----------|---------|---------|---------|
| `CommandHandler` | Handles `/command` messages | Registered in `AnimemizerBotCommand` enum, matched by command name | `StartCommandHandler` |
| `CallbackHandler` | Handles inline button clicks | `canHandle(callbackData)` checks prefix via `AnimemizerBotCallback` enum | `NextEpisodeCallbackHandler` |
| `TextHandler` | Handles plain text and `/search` | `canHandle(messageText)` with flexible matching | `SearchTextHandler` |

### Adding a New Handler

1. Create in `bot/handler/command/`, `bot/handler/callback/`, or `bot/handler/text/`
2. Implement `CommandHandler`, `CallbackHandler`, or `TextHandler` interface
3. For commands: add to `AnimemizerBotCommand` enum
4. For callbacks: use `AnimemizerBotCallback` enum for prefix-based routing
5. Spring auto-discovers via `@Component` with list injection in `BotUpdateProcessor`

### Adding a New Integration (Watch Link Resolver)

1. Create package: `integration/{provider}/`
2. DTOs in `dto/` as Records matching the provider's API response
3. `{Provider}Client` — HTTP interface using `@HttpExchange` + `@PostExchange`/`@GetExchange` (for JSON APIs like Shikimori, Tavily)
4. `{Provider}WatchLinkResolver` — implements the `WatchLinkResolver` interface (`resolveWatchUrl(Anime)`)
5. Add provider config to `application.yaml`

### HTTP Client Pattern

For JSON API providers (Shikimori, Tavily), use Spring's declarative HTTP clients:
- Interface annotated with `@HttpExchange` and `@GetExchange`/`@PostExchange` methods
- Configured via `RestClient` beans and `HttpServiceProxyFactory` in `HttpClientConfig`
- Registered with `@ImportHttpServices`

### File Organization

| Purpose | Path |
|---------|------|
| App entry | `Application.java` |
| Bot routing | `bot/processor/BotUpdateProcessor.java` |
| Command handlers | `bot/handler/command/` |
| Callback handlers | `bot/handler/callback/` |
| Text handlers | `bot/handler/text/` |
| Message building | `bot/message/AnimeMessageBuilder.java` |
| Telegram utilities | `bot/message/TelegramMessageUtils.java` |
| Integrations | `integration/{shikimori,tavily}` |
| Domain models | `core/anime/` |
| Watchlist tracking | `core/watchlist/` |
| Telegram user state | `core/telegramuser/` |
| JSON utilities | `core/service/JsonService.java` |
| REST controllers | `controller/` (WebhookController) |
| Exception handling | `exception/GlobalExceptionHandler.java` |
| HTTP client config | `config/HttpClientConfig.java` |
| Cache config | `config/CacheConfig.java` |
| Config properties | `config/props/` (TelegramBotProperties, AnimeUrlProperties, TavilyProperties) |

## Testing Conventions

- **Framework:** JUnit 6 + AssertJ
- **Naming:** `should_[ExpectedBehavior]_when_[StateUnderTest]`
- **Structure:** BDD style — `// Given`, `// When`, `// Then` comments
- **Slicing:** Use `@DataJpaTest` or `@WebMvcTest` to limit Spring context
- **Database:** H2 via `application-test.yaml`, schema from `schema.sql`

```java
@Test
void should_ReturnOptionalWithAnime_when_IdExists() {
    // Given
    Long animeId = 1L;

    // When
    Optional<Anime> result = connector.findById(animeId);

    // Then
    assertThat(result).isPresent();
}
```

## Things to Avoid

- ❌ Field injection with `@Autowired`
- ❌ Catching generic exceptions (`Exception`, `RuntimeException`)
- ❌ Hard-coding retry logic (use Spring Retry or circuit breakers)
- ❌ Blocking in async handlers (`Thread.sleep()`, unbounded `.get()`)
- ❌ Skipping cache config for hot-path API calls
- ❌ Lombok `@Slf4j` — use SLF4J directly
- ❌ Mutable DTOs — use Records
- ❌ Using `var` for local variables — use explicit type names
