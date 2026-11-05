package com.ieltspath.game.api.exception;

import com.ieltspath.game.domain.exception.GameMatchNotFoundException;
import com.ieltspath.game.domain.exception.GameRoomNotFoundException;
import com.ieltspath.game.domain.exception.GameSessionNotFoundException;
import com.ieltspath.game.domain.exception.InvalidGameRoomStateException;
import com.ieltspath.game.domain.exception.InvalidGameSessionStateException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(com.ieltspath.game.domain.exception.SoloGameplayException.class)
    public ResponseEntity<java.util.Map<String, Object>> soloError(
            com.ieltspath.game.domain.exception.SoloGameplayException e,
            HttpServletRequest request) {
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("timestamp", Instant.now());
        body.put("status", e.status());
        body.put("error", HttpStatus.valueOf(e.status()).getReasonPhrase());
        body.put("code", e.code());
        body.put("message", e.getMessage());
        body.put("path", request.getRequestURI());
        body.put("details", e instanceof com.ieltspath.game.domain.exception.NotEnoughWordsException shortage
                ? java.util.Map.of("available", shortage.available()) : null);
        return ResponseEntity.status(e.status()).body(body);
    }

    @ExceptionHandler(com.ieltspath.game.domain.exception.GameRoomForbiddenException.class)
    public ResponseEntity<ErrorResponse> roomForbidden(
            RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage(), request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> malformedRequest(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request body or enum value", request, null);
    }

    @ExceptionHandler({
        GameSessionNotFoundException.class,
        GameRoomNotFoundException.class,
        GameMatchNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> notFound(
            RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request, null);
    }

    @ExceptionHandler({
        InvalidGameSessionStateException.class,
        InvalidGameRoomStateException.class,
        IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> badRequest(
            RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request, null);
    }

    @ExceptionHandler({IllegalStateException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> conflict(
            RuntimeException exception, HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT, "The game state conflicts with this request", request, null);
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErrorResponse> contentUnavailable(
            RestClientException exception, HttpServletRequest request) {
        return error(
                HttpStatus.SERVICE_UNAVAILABLE, "Content service is unavailable", request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        exception
                .getBindingResult()
                .getFieldErrors()
                .forEach(
                        fieldError ->
                                details.put(fieldError.getField(), fieldError.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", request, details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(
            Exception exception, HttpServletRequest request) {
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null);
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> details) {
        return ResponseEntity.status(status)
                .body(
                        new ErrorResponse(
                                Instant.now(),
                                status.value(),
                                status.getReasonPhrase(),
                                message,
                                request.getRequestURI(),
                                details));
    }
}
