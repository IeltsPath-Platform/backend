package com.ieltspath.notification.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<ErrorResponse> notFound(Exception e, HttpServletRequest r) {
        String message = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : "Resource not found";
        return error(HttpStatus.NOT_FOUND, message, r, null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorResponse> responseStatus(ResponseStatusException e, HttpServletRequest r) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = e.getReason() != null ? e.getReason() : e.getMessage();
        return error(status, message, r, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadableRequest(HttpMessageNotReadableException e, HttpServletRequest r) {
        return error(HttpStatus.BAD_REQUEST, "Request body contains an invalid or unreadable value", r, null);
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

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException e, HttpServletRequest r) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), r, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest r) {
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

