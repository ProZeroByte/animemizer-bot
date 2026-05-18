package pro.zerobyte.animemizerbot.exception;

/**
 * Thrown when JSON deserialization fails.
 */
public class JsonDeserializationException extends RuntimeException {
    public JsonDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}

