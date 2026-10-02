package com.group01.learning.api.exception;

import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.domain.exception.LearningGateException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(LearningGateException.class)
    public ResponseEntity<LearningErrorResponse> gate(LearningGateException exception) {
        var reviews = "REVIEW_REQUIRED".equals(exception.getCode()) ? exception.getReviews().stream()
                .map(review -> new LearningErrorResponse.Review(review.reviewId(), review.lessonId(),
                        review.knowledgePointId())).toList() : null;
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new LearningErrorResponse(exception.getMessage(), exception.getCode(), reviews));
    }

    @ExceptionHandler(LearningRequestException.class)
    public ResponseEntity<LearningErrorResponse> learningRequest(LearningRequestException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new LearningErrorResponse(exception.getMessage(), exception.getCode(), null,
                        exception.getSubmissionId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<LearningErrorResponse> validation(MethodArgumentNotValidException exception,
                                                     HttpServletRequest request) {
        return ResponseEntity.unprocessableEntity().body(new LearningErrorResponse("Invalid request", null, null));
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<LearningErrorResponse> badRequest(Exception exception, HttpServletRequest request) {
        return ResponseEntity.unprocessableEntity().body(new LearningErrorResponse("Invalid request", null, null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> forbidden(AccessDeniedException exception, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Access denied", request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(NoResourceFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Not found", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> methodNotAllowed(HttpRequestMethodNotSupportedException exception,
                                                          HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected request failure: type={}, correlationId={}",
                exception.getClass().getName(), request.getHeader("X-Correlation-Id"));
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred", request, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                HttpServletRequest request, Map<String, String> details) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI(), details));
    }
}
