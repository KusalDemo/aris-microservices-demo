package com.aris.order.config;

import com.aris.common.aris.ArisCallException;
import com.aris.order.service.DemoStatsService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class OrderExceptionHandler {

    private final DemoStatsService demoStatsService;

    public OrderExceptionHandler(DemoStatsService demoStatsService) {
        this.demoStatsService = demoStatsService;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setTitle("Validation failed");
        FieldError fieldError = ex.getBindingResult().getFieldError();
        detail.setDetail(fieldError != null
                ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                : "Invalid request");
        detail.setProperty("retriesObserved", 0);
        return detail;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleStatus(ResponseStatusException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(ex.getStatusCode());
        detail.setTitle(ex.getStatusCode().toString());
        detail.setDetail(ex.getReason());
        detail.setProperty("retriesObserved", resolveRetries(ex));
        return detail;
    }

    private int resolveRetries(ResponseStatusException ex) {
        ProblemDetail body = ex.getBody();
        if (body != null) {
            Map<String, Object> props = body.getProperties();
            if (props != null) {
                Integer fromBody = asNonNegativeInt(props.get("retriesObserved"));
                if (fromBody != null) {
                    return fromBody;
                }
            }
        }
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof ArisCallException ace) {
                return Math.max(0, ace.getRetryAttempts());
            }
            cause = cause.getCause();
        }
        return Math.max(0, demoStatsService.getLastRetriesObserved());
    }

    private static Integer asNonNegativeInt(Object value) {
        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Math.max(0, Integer.parseInt(s.trim()));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
