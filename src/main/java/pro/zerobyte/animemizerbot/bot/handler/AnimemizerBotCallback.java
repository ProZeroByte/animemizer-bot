package pro.zerobyte.animemizerbot.bot.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public enum AnimemizerBotCallback {
    NEXT_EPISODE("NEXT_EPISODE"),
    PREVIOUS_EPISODE("PREVIOUS_EPISODE"),
    LOAD_SEQUEL("LOAD_SEQUEL"),
    LOAD_PREQUEL("LOAD_PREQUEL"),
    SET_WATCH_STATUS("SET_WATCH_STATUS"),
    LIST_PAGE("LIST_PAGE");

    private static final Logger log = LoggerFactory.getLogger(AnimemizerBotCallback.class);
    private static final String SEPARATOR = ":";
    private final String prefix;

    AnimemizerBotCallback(String rawPrefix) {
        this.prefix = rawPrefix + SEPARATOR;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String toCallbackData(Object id) {
        return this.prefix + id;
    }

    /**
     * Builds composite callback data for SET_WATCH_STATUS: animeId + statusName separated by SEPARATOR.
     */
    public String toWatchStatusCallbackData(long animeId, String statusName) {
        return this.prefix + animeId + SEPARATOR + statusName;
    }

    public boolean isCallbackSupported(String callbackData){
        return callbackData != null && callbackData.startsWith(this.prefix);
    }

    public Long extractId(String callbackData) {
        int startOffset = this.prefix.length();
        if (startOffset == callbackData.length()) {
            log.warn("Attempted to extract ID, but payload is empty. CallbackData: '{}', Expected prefix: '{}'", callbackData, this.prefix);
            return null;
        }

        try {
            return Long.parseLong(callbackData, startOffset, callbackData.length(), 10);
        } catch (NumberFormatException e) {
            log.warn("Invalid ID format in callbackData: '{}'. Expected a numeric ID. Error: {}", callbackData, e.getMessage());
            return null;
        }
    }


    /**
     * Record для удобного хранения и передачи разобранного колбэка.
     */

    public record Payload(AnimemizerBotCallback action, String id) {}

    /**
     * Разбирает входящую строку callbackData один раз и возвращает Payload.
     */
    public static Optional<Payload> parse(String data) {
        if (data == null) {
            return Optional.empty();
        }

        int separatorIndex = data.indexOf(SEPARATOR);

        // Если разделителя нет, колбэк невалиден (или можно вернуть Payload с id = null, если бывают колбэки без ID)
        if (separatorIndex == -1) {
            return Optional.empty();
        }

        String enumName = data.substring(0, separatorIndex);
        String id = data.substring(separatorIndex + 1);

        try {
            // Используем unnamed variable '_' (доступно с Java 22+)
            return Optional.of(new Payload(valueOf(enumName), id));
        } catch (IllegalArgumentException _) {
            // Enum с таким именем не найден
            return Optional.empty();
        }
    }

    public record WatchLinkPayload(String providerCode, String publicId) {}

    public static Optional<WatchLinkPayload> extractWatchLinkPayload(String id) {
        if (id == null) {
            return Optional.empty();
        }

        int separatorIndex = id.indexOf(SEPARATOR);
        if (separatorIndex == -1) {
            return Optional.empty();
        }

        return Optional.of(new WatchLinkPayload(id.substring(0, separatorIndex), id.substring(separatorIndex + 1)));
    }
}
