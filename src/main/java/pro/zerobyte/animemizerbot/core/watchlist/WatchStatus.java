package pro.zerobyte.animemizerbot.core.watchlist;

import java.util.Set;

public enum WatchStatus {
    WATCHING("Watching", "Смотрю"),
    PLANNED("Planned", "В планах"),
    COMPLETED("Completed", "Просмотрено"),
    ON_HOLD("On hold", "Отложено"),
    DROPPED("Dropped", "Брошено");

    private static final Set<WatchStatus> TERMINAL_STATUSES = Set.of(COMPLETED, DROPPED);

    private final String englishName;
    private final String russianName;

    WatchStatus(String englishName, String russianName) {
        this.englishName = englishName;
        this.russianName = russianName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getRussianName() {
        return russianName;
    }

    /**
     * Returns true if the status is a terminal state (no further episode tracking expected).
     */
    public boolean isTerminal() {
        return TERMINAL_STATUSES.contains(this);
    }

    /**
     * Determines the appropriate status when all episodes have been watched.
     * Returns COMPLETED if currently WATCHING, otherwise keeps the current status.
     */
    public WatchStatus onAllEpisodesWatched() {
        return switch (this) {
            case WATCHING -> COMPLETED;
            default -> this;
        };
    }
}
