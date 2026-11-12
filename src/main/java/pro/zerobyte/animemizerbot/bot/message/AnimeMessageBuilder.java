package pro.zerobyte.animemizerbot.bot.message;

import pro.zerobyte.animemizerbot.bot.handler.AnimemizerBotCallback;
import pro.zerobyte.animemizerbot.core.anime.Anime;
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

        InlineKeyboardRow watchRow = createWatchRow(anime);
        if (!watchRow.isEmpty()) {
            rows.add(watchRow);
        }

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

    private InlineKeyboardRow createWatchRow(Anime anime) {
        if (anime.watchUrl() == null || anime.watchUrl().isBlank()) {
            return new InlineKeyboardRow();
        }

        return new InlineKeyboardRow(InlineKeyboardButton.builder()
                .text("Смотреть")
                .url(anime.watchUrl())
                .build());
    }
}
