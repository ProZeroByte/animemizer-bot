package pro.zerobyte.animemizerbot.core.anime;

import java.util.Optional;

public enum AnimeSource {
    SHIKIMORI("shikimori_"),
    ANILIBRIA("anilibria_"),
    ANIMEGO("animego_"),
    YUMMYANIME("yummyanime_");

    private final String prefix;
    private final int prefixLength;

    AnimeSource(String prefix) {
        this.prefix = prefix;
        this.prefixLength = prefix.length();
    }

    public String createPublicId(long id) {
        return this.prefix + id;
    }

    public Optional<Long> extractIdFromPublicId(String publicId) {
        return Optional.ofNullable(publicId)
                .filter(id -> id.length() > this.prefixLength)
                .map(id -> id.substring(this.prefixLength))
                .flatMap(idPart -> {
                    try {
                        return Optional.of(Long.valueOf(idPart));
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                });
    }
}
