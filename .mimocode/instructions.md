# Mimo AI — Animemizer Bot Context

## Project Overview

Telegram bot for anime discovery. Integrates:
- **Shikimori** — primary source, full anime data via REST API
- **Tavily** — watch link resolution via web search

## Tech Stack

- **Java 26** — virtual threads enabled by default, Records, pattern matching, Stream Gatherers
- **Spring Boot 4.0.6** / Spring Framework 7.0.7
- **Jetty** (NOT Tomcat) — Tomcat is excluded in pom.xml
- **Jackson 3.1.1** (`jackson.version` property) — this is Jackson 3.x, NOT 2.x
- **PostgreSQL** (prod) / **H2** (test) via `spring-boot-starter-data-jdbc` (NOT JPA)
- **Caffeine** cache — 3 named caches in `CacheConfig.java`
- **telegrambots 9.6.0** — long-polling + webhook
- **Jsoup** for HTML scraping (legacy — no longer used by active integrations)
- **JUnit 6** + AssertJ for testing

## Build & Run

```powershell
mvn clean package                                            # build
mvn test                                                     # tests (H2 + application-test.yaml)
java -jar target/animemizer-bot-*.jar --spring.profiles.active=dev   # long polling
java -jar target/animemizer-bot-*.jar --spring.profiles.active=prod  # webhook
docker build -t animemizer-bot:latest . && docker-compose up -d      # Docker
```

No single-test shortcut — use `mvn test -Dtest=ClassName`.

## Environment Variables

Required: `TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_USERNAME`, `TELEGRAM_BOT_API_SECRET_TOKEN`, `DATASOURCE_URL`, `DATASOURCE_USERNAME`, `DATASOURCE_PASSWORD`

Prod-only: `TELEGRAM_BOT_WEBHOOK_PATH`, `TELEGRAM_BOT_WEBHOOK_BASE_URL`

Tests use dummy values in `application-test.yaml` — no env vars needed for `mvn test`.

## Architecture

### Update Processing Pipeline

```
Telegram Update → PollingBot / WebhookController
  → BotUpdateProcessor (routing via pattern matching on Update type)
    → TextHandler (plain text / /search)  ← checked FIRST
    → CallbackHandler (inline button clicks)
    → CommandHandler (/start, /help, etc.)
      → Business logic (service layer)
        → Telegram API response
```

**Critical:** TextHandlers are checked BEFORE CommandHandlers. `SearchTextHandler` catches all text including `/search`, so it must return `false` for other commands.

Handler return type is `PartialBotApiMethod<?>`. Returning `null` means "I'm managing my own Telegram responses" (placeholder-then-replace pattern).

### Handler Types

| Interface | Purpose | Routing |
|-----------|---------|---------|
| `CommandHandler` | `/command` messages | `AnimemizerBotCommand` enum, matched by command name |
| `CallbackHandler` | Inline button clicks | `AnimemizerBotCallback` prefix (e.g., `NEXT_EPISODE:`) |
| `TextHandler` | Plain text and `/search` | `canHandle(messageText)` predicate |

### Callback Data Format

All callbacks: `PREFIX:payload` managed by `AnimemizerBotCallback` enum.
- Simple: `NEXT_EPISODE:123`
- Composite: `SET_WATCH_STATUS:123:WATCHING`
- Watch link: `providerCode:publicId` (parsed by `extractWatchLinkPayload`)

### Placeholder-then-Replace Pattern

For slow operations (search, navigation, list):
1. Send placeholder message immediately
2. Do async work
3. Edit placeholder in-place (`EditMessageText` / `EditMessageCaption` / `EditMessageMedia`)
4. If placeholder send fails, return fallback result directly

### Watch Link Enrichment

`AnimeService.enrichAndSaveIfRequired()` resolves a single watch link via `WatchLinkResolver` (Tavily) on every anime fetch. Failed lookups cached 24h in `providerFailures`.

### Domain Model

- All entities are **Records** with `@Table` / `@MappedCollection` (Spring Data JDBC, NOT JPA)
- `Anime` has `withId()`, `withDescription()`, `withLinks()`, `withWatchUrl()` copy methods (immutable Records)
- `AnimeSource` prefix: `shikimori_` — use `createPublicId()` / `extractIdFromPublicId()`
- `WatchStatus` has transition logic: `onAllEpisodesWatched()`, `onRewatchStarted()`, `isTerminal()`

### HTTP Client Setup

`HttpClientConfig` manually builds `RestClient` → `RestClientAdapter` → `HttpServiceProxyFactory` → client proxy for each `@HttpExchange` interface. Registered with `@ImportHttpServices`. Spring's declarative HTTP client pattern, NOT Feign.

### WebConfig

Custom `AbstractHttpMessageConverter` routes `BotApiMethod` and `Update` serialization through `JsonService` (Jackson 3.x `ObjectMapper`).

## Mandatory Coding Rules

### Dependency Injection
- **NEVER** use `@Autowired` on fields. Always constructor injection with `final` fields.
- Single constructor — no `@Autowired` annotation needed.

### DTOs
- All DTOs must be Java **Records** — immutable, no Lombok.

### Error Handling
- **NEVER** catch `Exception` or `RuntimeException` — catch specific types.
- Return `Optional.empty()` on failures; log the specific exception.
- Use `@RestControllerAdvice` (`GlobalExceptionHandler`) for centralized handling.

### Logging
- SLF4J with `LoggerFactory` — **no** Lombok `@Slf4j`.
- Levels: `ERROR` for failures, `WARN` for suspicious states, `DEBUG` for flow tracing.

### Async Patterns
- `CompletableFuture` with `@Qualifier("applicationTaskExecutor")` virtual thread executor.
- `@Cacheable` for expensive operations (searches, external API calls).
- `thenCombine()` for fan-out parallel requests.
- **NEVER** block in async handlers (`Thread.sleep()`, `.get()` without timeout).

### Variable Declarations
- **NEVER** use `var`. Always explicit type names:

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

### Stream Gatherers
- `Gatherers.windowFixed(n)` for partitioning (e.g., status buttons in rows of 3, watch link buttons in rows of 2)

## File Organization

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
| REST controllers | `controller/` |
| Exception handling | `exception/GlobalExceptionHandler.java` |
| HTTP client config | `config/HttpClientConfig.java` |
| Cache config | `config/CacheConfig.java` |
| Config properties | `config/props/` |

## Testing Conventions

### Framework & Libraries
- **JUnit 6** + **AssertJ** — the only assertion library allowed
- **Mockito** for mocking (inline mock maker)
- **H2** in-memory database for integration tests via `application-test.yaml`
- Never mix assertion libraries in a single test class

### Test Naming
- Method names follow `should_[ExpectedBehavior]_when_[StateUnderTest]` pattern
- Use underscores to separate words, `should` and `when` are lowercase
- Examples: `should_returnEmpty_when_AnimeNotFound`, `should_setStatusToCompleted_when_allEpisodesWatched`
- ❌ Do NOT use: `methodName_shouldX` or `camelCaseShouldX` naming

### Test Structure (BDD Style)
Every test method must have these comment markers:
```java
@Test
void should_returnWatchlist_when_entryExists() {
    // Given
    Watchlist wl = newWatchlist(USER_ID, ANIME_ID, WatchStatus.WATCHING, 5);
    when(repository.findByTelegramUserIdAndAnimeId(USER_ID, ANIME_ID)).thenReturn(Optional.of(wl));

    // When
    Optional<Watchlist> result = service.getWatchlist(USER_ID, ANIME_ID);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().lastWatchedEpisode()).isEqualTo(5);
}
```

### Grouping with @Nested
Group tests by method-under-test using `@Nested` + `@DisplayName`:
```java
class WatchlistServiceTest {

    @Nested
    @DisplayName("getWatchlist")
    class GetWatchlist {

        @Test
        void should_returnWatchlist_when_entryExists() {
            // Given ... When ... Then ...
        }

        @Test
        void should_returnEmpty_when_entryDoesNotExist() {
            // Given ... When ... Then ...
        }
    }

    @Nested
    @DisplayName("incrementEpisode")
    class IncrementEpisode {
        // ...
    }
}
```

### Test Categories

#### 1. Pure Unit Tests (no Spring context)
Use for: utility classes, static methods, domain logic.
Setup: manual `mock()` + constructor instantiation in `@BeforeEach`.
```java
class WatchlistServiceTest {
    private WatchlistRepository repository;
    private AnimeService animeService;
    private WatchlistService service;

    @BeforeEach
    void setUp() {
        repository = mock(WatchlistRepository.class);
        animeService = mock(AnimeService.class);
        service = new WatchlistService(repository, animeService);
    }
}
```
Preferred for service classes where dependencies can be mocked directly.

#### 2. Mockito Unit Tests (@ExtendWith)
Use for: connectors, mappers, components with many dependencies.
Setup: `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`.
```java
@ExtendWith(MockitoExtension.class)
class ShikimoriConnectorTest {

    @Mock
    private ShikimoriClient shikimoriClient;

    @InjectMocks
    private ShikimoriConnector shikimoriConnector;
}
```
Use `@BeforeEach` for shared test data setup.

#### 3. Spring Integration Tests
Use for: repository tests, mapper tests requiring Jackson, full context tests.
Setup: `@SpringBootTest` + `@ActiveProfiles("test")`.
```java
@SpringBootTest
@ActiveProfiles("test")
class ShikimoriMapperTest {

    @Autowired
    private ShikimoriMapper mapper;

    @Autowired
    private ObjectMapper objectMapper;
}
```
Use `@DataJpaTest` for repository-only tests when schema is complete.

### Assertion Rules
- Always use **AssertJ**: `assertThat(result).isPresent()`, `assertThat(value).isEqualTo(expected)`
- ❌ Never use JUnit assertions: `assertTrue`, `assertEquals`, `assertNotNull`
- Use fluent assertions: `.isEmpty()`, `.isPresent()`, `.hasSize()`, `.contains()`, `.doesNotContain()`

### Mockito Verification Patterns
```java
verify(shikimoriClient, times(1)).searchAnime("Test Anime", 1);
verify(shikimoriClient, never()).getAnimeDetails(anyInt());
verify(repository, atLeastOnce()).save(any(Watchlist.class));
```

### Helper Methods for Test Boilerplate
Extract repetitive setup into private helper methods within the test class:
```java
private Watchlist newWatchlist(Long userId, Long animeId, WatchStatus status, int lastWatchedEpisode) {
    return new Watchlist(1L, userId, animeId, status, lastWatchedEpisode, NOW, NOW);
}

private void stubSaveToReturnSame() {
    when(repository.save(any(Watchlist.class))).thenAnswer(inv -> inv.getArgument(0));
}
```

### Test Constants
Define reusable constants as `private static final` fields:
```java
private static final Long USER_ID = 100L;
private static final Long ANIME_ID = 200L;
private static final LocalDateTime NOW = LocalDateTime.now();
```

### JSON Fixtures
For mapper/deserialization tests, load JSON from classpath:
```java
try (InputStream is = new ClassPathResource("json/shikimori/shikimori_anime_dto.json").getInputStream()) {
    ShikimoriAnimeDto dto = objectMapper.readValue(is, ShikimoriAnimeDto.class);
    // ...
}
```
Fixture location: `src/test/resources/json/{provider}/`

### Test File Organization
| Test type | File name | Location |
|-----------|-----------|----------|
| Unit test | `{ClassName}Test.java` | Same package in `src/test/java/` |
| Integration test | `{ClassName}IntegrationTest.java` | Same package in `src/test/java/` |
| JSON fixtures | `*.json` | `src/test/resources/json/{provider}/` |
| Test schema | `schema.sql` | `src/test/resources/` |
| Test config | `application-test.yaml` | `src/test/resources/` |

### Things to Avoid in Tests
- ❌ `var` in test code — always use explicit type names
- ❌ `@Autowired` field injection in unit tests — use constructor or manual instantiation
- ❌ JUnit assertions (`assertTrue`, `assertEquals`, `assertNotNull`) — use AssertJ (`assertThat`)
- ❌ Hitting real external APIs — use mocks or WireMock
- ❌ `@SpringBootTest` for pure unit tests — prefer MockitoExtension or manual mocking
- ❌ Mixing assertion libraries (JUnit + AssertJ) in a single test class
- ❌ CamelCase naming (`findByTitle_shouldX`) — use `should_X_when_Y` consistently
- ❌ Missing BDD comments (`// Given`, `// When`, `// Then`)
- ❌ `@DisplayName` on individual test methods — use only on `@Nested` classes
