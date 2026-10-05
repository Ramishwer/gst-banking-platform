package api.auth_service.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse (
        int status,
        String error,
        String message,
        Map<String, String> validationErrors,
        LocalDateTime timestamp)
{ }
