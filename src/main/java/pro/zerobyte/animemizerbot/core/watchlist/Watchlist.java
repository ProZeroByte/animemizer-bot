package pro.zerobyte.animemizerbot.core.watchlist;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("watchlist")
public record Watchlist(
        @Id Long id,
        Long telegramUserId,
        Long animeId,
        WatchStatus status,
        Integer lastWatchedEpisode,
        LocalDateTime addedAt,
        LocalDateTime updatedAt
) {}
