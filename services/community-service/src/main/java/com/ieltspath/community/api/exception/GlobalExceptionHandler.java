package com.ieltspath.community.api.exception;

import com.ieltspath.community.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(CommunityUnavailableException.class)
    ResponseEntity<ErrorResponse> unavailable(Exception e, HttpServletRequest r) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), r, null);
    }
    @ExceptionHandler({CommunityNotFoundException.class, NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<ErrorResponse> notFound(Exception e, HttpServletRequest r) {
        String message = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : "Resource not found";
        return error(HttpStatus.NOT_FOUND, message, r, null);
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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest r) {
        String name = e.getName() != null && !e.getName().isBlank() ? e.getName() : "parameter";
        return error(HttpStatus.BAD_REQUEST, "Invalid value for " + name, r, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> constraintViolation(ConstraintViolationException e, HttpServletRequest r) {
        return error(HttpStatus.BAD_REQUEST, "Constraint violation", r, null);
    }

    @ExceptionHandler(com.ieltspath.commonstorage.UploadRejectedException.class)
    ResponseEntity<ErrorResponse> uploadRejected(com.ieltspath.commonstorage.UploadRejectedException e, HttpServletRequest r) {
        HttpStatus status = switch (e.reason()) {
            case TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE;
            case TYPE_NOT_ALLOWED -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            default -> HttpStatus.BAD_REQUEST;
        };
        return error(status, e.getMessage(), r, null);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> maxUploadSize(org.springframework.web.multipart.MaxUploadSizeExceededException e, HttpServletRequest r) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "File size exceeds configured limit", r, null);
    }

    @ExceptionHandler(CommunityRateLimitExceededException.class)
    ResponseEntity<ErrorResponse> rateLimited(CommunityRateLimitExceededException e, HttpServletRequest r) {
        return error(HttpStatus.TOO_MANY_REQUESTS, e.getMessage(), r, null);
    }

    @ExceptionHandler(com.ieltspath.commonstorage.StorageException.class)
    ResponseEntity<ErrorResponse> storage(com.ieltspath.commonstorage.StorageException e, HttpServletRequest r) {
        if (e.reason() == com.ieltspath.commonstorage.StorageException.Reason.UNAVAILABLE) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "Storage service is currently unavailable", r, null);
        }
        LOG.error("Storage operation failed operation={} reason={} ref={}", e.operation(), e.reason(), e.ref());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Storage operation failed", r, null);
    }

    @ExceptionHandler({CommunityException.class, IllegalArgumentException.class})
    ResponseEntity<ErrorResponse> badRequest(Exception e, HttpServletRequest r) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), r, null);
    }

    @ExceptionHandler(CommunityConflictException.class)
    ResponseEntity<ErrorResponse> communityConflict(CommunityConflictException e, HttpServletRequest r) {
        return error(HttpStatus.CONFLICT, e.getMessage(), r, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadableRequest(HttpMessageNotReadableException e, HttpServletRequest r) {
        return error(HttpStatus.BAD_REQUEST, "Request body contains an invalid or unreadable value", r, null);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    ResponseEntity<ErrorResponse> conflict(Exception e, HttpServletRequest r) {
        return error(HttpStatus.CONFLICT, "The resource changed concurrently; retry with fresh data", r, null);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> missingParam(org.springframework.web.bind.MissingServletRequestParameterException e, HttpServletRequest r) {
        return error(HttpStatus.BAD_REQUEST, "Required parameter is missing: " + e.getParameterName(), r, null);
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> unsupportedMediaType(org.springframework.web.HttpMediaTypeNotSupportedException e, HttpServletRequest r) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getMessage(), r, null);
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<ErrorResponse> notAcceptable(org.springframework.web.HttpMediaTypeNotAcceptableException e, HttpServletRequest r) {
        return error(HttpStatus.NOT_ACCEPTABLE, e.getMessage(), r, null);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> methodNotAllowed(org.springframework.web.HttpRequestMethodNotSupportedException e, HttpServletRequest r) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, e.getMessage(), r, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest r) {
        LOG.error(
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
