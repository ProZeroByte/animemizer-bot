package pro.zerobyte.animemizerbot.bot.message;

import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCallback;
import pro.zerobyte.animemizerbot.config.props.AnimeUrlProperties;
import pro.zerobyte.animemizerbot.core.anime.Anime;
import pro.zerobyte.animemizerbot.core.anime.AnimeLink;
import pro.zerobyte.animemizerbot.core.anime.AnimeSource;
import pro.zerobyte.animemizerbot.core.watchlist.WatchStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Gatherers;

@Component
public class AnimeMessageBuilder {

    private final AnimeUrlProperties animeUrlProperties;

    public AnimeMessageBuilder(AnimeUrlProperties animeUrlProperties) {
        this.animeUrlProperties = animeUrlProperties;
    }

    public @NonNull String buildCaption(@NonNull Anime anime, Integer userWatchedEpisodes, @Nullable WatchStatus watchStatus) {
        String title = Objects.requireNonNullElse(anime.russianName(), anime.name());

        String totalEpisodes = (anime.episodes() != null && anime.episodes() > 0)
                ? String.valueOf(anime.episodes())
                : "?";

        int watchedCount = Objects.requireNonNullElse(userWatchedEpisodes, 0);
        String eps = "%s / %s".formatted(watchedCount, totalEpisodes);

        String releaseStatus = getReleaseStatusDisplay(anime);
        String watchStatusDisplay = getWatchStatusDisplay(watchStatus);

        StringBuilder sb = new StringBuilder();
        sb.append("🎬 *%s*%n".formatted(title));
        if (anime.airedOn() != null) {
            sb.append("%n📅 Год: %s".formatted(anime.airedOn().getYear()));
        }
        sb.append("%n📺 Релиз: %s".formatted(releaseStatus));
        if (Boolean.TRUE.equals(anime.isOngoing()) && anime.episodesAired() != null && anime.episodesAired() > 0) {
            String airedTotal = (anime.episodes() != null && anime.episodes() > 0)
                    ? "%d из %d".formatted(anime.episodesAired(), anime.episodes())
                    : "%d".formatted(anime.episodesAired());
            sb.append("%n🎞 Эпизоды: %s".formatted(airedTotal));
        }
        if (watchStatusDisplay != null) {
            sb.append("%n📊 Статус: %s".formatted(watchStatusDisplay));
        }
        sb.append("%n👁 Просмотрено: %s".formatted(eps));

        return sb.toString();
    }

    private String getReleaseStatusDisplay(@NonNull Anime anime) {
        return switch (anime) {
            case Anime a when Boolean.TRUE.equals(a.isAnons()) -> "Анонс 🔵";
            case Anime a when Boolean.TRUE.equals(a.isOngoing()) -> "В онгоинге 🟢";
            case Anime a when Boolean.TRUE.equals(a.isReleased()) -> "Вышло 🏁";
            default -> "Неизвестно ❓";
        };
    }

    private @Nullable String getWatchStatusDisplay(@Nullable WatchStatus watchStatus) {
        if (watchStatus == null) {
            return null;
        }
        return switch (watchStatus) {
            case WATCHING -> "Смотрю 👀";
            case COMPLETED -> "Просмотрено ✅";
            case PLANNED -> "В планах 📋";
            case REWATCHING -> "Пересматриваю 🔄";
            case ON_HOLD -> "Отложено ⏸";
            case DROPPED -> "Брошено ❌";
        };
    }

    public InlineKeyboardMarkup createInlineKeyboard(@NonNull Anime anime, int watchedEpisodes) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(createEpisodeRow(anime, watchedEpisodes));

        InlineKeyboardRow seasonRow = createSeasonRow(anime);
        if (!seasonRow.isEmpty()) {
            rows.add(seasonRow);
        }

        rows.addAll(createStatusRows(anime));

        rows.addAll(createWatchLinkRows(anime));

        return new InlineKeyboardMarkup(rows);
    }

    private InlineKeyboardRow createEpisodeRow(Anime anime, int watchedEpisodes) {
        boolean allWatched = anime.episodes() != null && anime.episodes() > 0
                && watchedEpisodes >= anime.episodes();

        InlineKeyboardButton minusBtn = InlineKeyboardButton.builder()
                .text("-1 серия")
                .callbackData(AnimemizerBotCallback.PREVIOUS_EPISODE.toCallbackData(anime.id()))
                .build();

        if (allWatched) {
            return new InlineKeyboardRow(minusBtn);
        }

        InlineKeyboardButton plusBtn = InlineKeyboardButton.builder()
                .text("+1 серия")
                .callbackData(AnimemizerBotCallback.NEXT_EPISODE.toCallbackData(anime.id()))
                .build();

        return new InlineKeyboardRow(minusBtn, plusBtn);
    }

    private InlineKeyboardRow createSeasonRow(Anime anime) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        if (anime.prequelPublicId() != null) {
            row.add(InlineKeyboardButton.builder()
                    .text("⬅️ Предыстория")
                    .callbackData(AnimemizerBotCallback.LOAD_PREQUEL.toCallbackData(anime.id()))
                    .build());
        }
        if (anime.sequelPublicId() != null) {
            row.add(InlineKeyboardButton.builder()
                    .text("Продолжение ➡️")
                    .callbackData(AnimemizerBotCallback.LOAD_SEQUEL.toCallbackData(anime.id()))
                    .build());
        }
        return row;
    }

    private List<InlineKeyboardRow> createStatusRows(Anime anime) {
        WatchStatus[] statuses = WatchStatus.values();
        return java.util.Arrays.stream(statuses)
                .gather(Gatherers.windowFixed(3))
                .map(window -> window.stream()
                        .map(status -> InlineKeyboardButton.builder()
                                .text(getWatchStatusEmoji(status))
                                .callbackData(AnimemizerBotCallback.SET_WATCH_STATUS.toWatchStatusCallbackData(anime.id(), status.name()))
                                .build())
                        .toList())
                .map(InlineKeyboardRow::new)
                .toList();
    }

    private String getWatchStatusEmoji(WatchStatus status) {
        return switch (status) {
            case WATCHING -> "👀";
            case PLANNED -> "📋";
            case COMPLETED -> "✅";
            case REWATCHING -> "🔄";
            case ON_HOLD -> "⏸";
            case DROPPED -> "❌";
        };
    }

    private List<InlineKeyboardRow> createWatchLinkRows(Anime anime) {
        if (anime.links() == null || anime.links().isEmpty()) {
            return List.of();
        }

        // Автоматическая разбивка кнопок по 2 в ряд с помощью Stream Gatherers
        return anime.links().stream()
                .map(AnimeLink::source)
                .distinct() // Защита от дубликатов источников
                .filter(this::isSupportedSource)
                .map(source -> InlineKeyboardButton.builder()
                        .text(getSourceDisplayName(source))
                        .url(animeUrlProperties.watchUrlTemplate().formatted(source.name(), anime.publicId()))
                        .build())
                .gather(Gatherers.windowFixed(2)) // Группируем элементы списка по 2
                .map(InlineKeyboardRow::new)
                .toList();
    }

    private boolean isSupportedSource(AnimeSource source) {
        return switch (source) {
            case ANIMEGO, ANILIBRIA, YUMMYANIME -> true;
            default -> false;
        };
    }

    private String getSourceDisplayName(AnimeSource source) {
        return switch (source) {
            case ANIMEGO -> "▶️ AnimeGO";
            case ANILIBRIA -> "▶️ AniLiberty";
            case YUMMYANIME -> "▶️ YummyAnime";
            default -> "▶️ " + source.name();
        };
    }
}
