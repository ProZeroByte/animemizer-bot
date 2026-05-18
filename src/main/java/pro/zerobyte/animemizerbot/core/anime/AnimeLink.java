package pro.zerobyte.animemizerbot.core.anime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("anime_link")
public record AnimeLink(
        @Id Long id,
        Long animeId,
        AnimeSource source,
        String externalId
) {}
