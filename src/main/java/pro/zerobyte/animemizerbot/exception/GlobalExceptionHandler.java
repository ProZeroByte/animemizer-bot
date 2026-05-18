package pro.zerobyte.animemizerbot.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        String realIp = resolveRealIp(request);
        String userAgent = resolveUserAgent(request);

        log.warn("Resource not found: {} {} | Real IP: {} | User-Agent: {}",
                request.getMethod(), request.getRequestURI(), realIp, userAgent);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public void handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String realIp = resolveRealIp(request);
        String userAgent = resolveUserAgent(request);

        log.warn("Method Not Allowed: {} for URI: {} | Real IP: {} | User-Agent: {}",
                ex.getMethod(), request.getRequestURI(), realIp, userAgent);
    }

    @ExceptionHandler({IllegalArgumentException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleBadRequest(Exception ex, HttpServletRequest request) {
        String realIp = resolveRealIp(request);
        String userAgent = resolveUserAgent(request);

        log.warn("Bad Request: {} {} | Real IP: {} | User-Agent: {} | Error: {}",
                request.getMethod(), request.getRequestURI(), realIp, userAgent, ex.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Bad Request", "message", ex.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, String>> handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        String realIp = resolveRealIp(request);
        String userAgent = resolveUserAgent(request);

        log.error("Database error: {} {} | Real IP: {} | User-Agent: {} | Error: {}",
                request.getMethod(), request.getRequestURI(), realIp, userAgent, ex.getMessage(), ex);
        return ResponseEntity.internalServerError()
                .body(Map.of("error", "Internal Server Error", "message", "A database error occurred"));
    }

    @ExceptionHandler(TelegramApiException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ResponseEntity<Map<String, String>> handleTelegramApi(TelegramApiException ex, HttpServletRequest request) {
        String realIp = resolveRealIp(request);
        String userAgent = resolveUserAgent(request);

        log.error("Telegram API error: {} {} | Real IP: {} | User-Agent: {} | Error: {}",
                request.getMethod(), request.getRequestURI(), realIp, userAgent, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "Bad Gateway", "message", "Telegram API is temporarily unavailable"));
    }

    private String resolveRealIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        return Stream.of(
                        request.getHeader("CF-Connecting-IP"),
                        xForwarded != null && !xForwarded.isBlank() ? xForwarded.split(",")[0].trim() : null,
                        request.getRemoteAddr())
                .filter(ip -> ip != null && !ip.isBlank())
                .findFirst()
                .orElse("UNKNOWN");
    }

    private String resolveUserAgent(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("User-Agent"))
                .filter(Predicate.not(String::isBlank))
                .orElse("UNKNOWN");
    }
}
