package pro.zerobyte.animemizerbot.exception;

/**
 * Thrown when JSON serialization fails.
 */
public class JsonSerializationException extends RuntimeException {
    public JsonSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}

