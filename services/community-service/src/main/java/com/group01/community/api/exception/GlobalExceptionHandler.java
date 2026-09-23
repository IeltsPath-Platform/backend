package com.group01.community.api.exception;

import com.group01.community.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CommunityNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(Exception e, HttpServletRequest r) {
        return error(HttpStatus.NOT_FOUND, e.getMessage(), r, null);
    }

    @ExceptionHandler({CommunityForbiddenException.class, AccessDeniedException.class})
    ResponseEntity<ErrorResponse> forbidden(Exception e, HttpServletRequest r) {
        return error(HttpStatus.FORBIDDEN, "Forbidden", r, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e, HttpServletRequest r) {
        Map<String, String> d = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> d.put(f.getField(), f.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", r, d);
    }

    @ExceptionHandler({CommunityException.class, IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> badRequest(Exception e, HttpServletRequest r) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), r, null);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    ResponseEntity<ErrorResponse> conflict(Exception e, HttpServletRequest r) {
        return error(HttpStatus.CONFLICT, "The resource changed concurrently; retry with fresh data", r, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest r) {
        log.error(
                "Unexpected community error correlationId={} path={}",
                r.getHeader("X-Correlation-Id"),
                r.getRequestURI(),
                e
        );
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected system error", r, null);
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> details
    ) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                details
        );
        return ResponseEntity.status(status).body(response);
    }
}
