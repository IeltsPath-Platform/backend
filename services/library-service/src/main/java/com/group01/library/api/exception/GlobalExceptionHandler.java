package com.group01.library.api.exception;

import com.group01.library.domain.exception.*;
import com.group01.library.application.exception.TopicServiceUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
            VocabularyNotFoundException.class,
            VideoNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFoundException(ContentDomainException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePersonalLibraryNotFound(ResourceNotFoundException ex,
                                                                         HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Không tìm thấy", request.getRequestURI(), null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handlePersonalLibraryConflict(ConflictException ex,
                                                                         HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "Dữ liệu đã tồn tại", request.getRequestURI(), null);
    }

    @ExceptionHandler({InvalidDataException.class, IllegalStateException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handlePersonalLibraryBadRequest(Exception ex,
                                                                           HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", request.getRequestURI(), null);
    }

    @ExceptionHandler({
            DuplicateCodeException.class,
            InvalidContentStateException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequestException(Exception ex, HttpServletRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex,
                                                                          HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                messageForPath(request, "Dữ liệu không hợp lệ", ex.getMessage()), request.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> details = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.put(error.getField(), error.getDefaultMessage());
        }
        return buildResponse(HttpStatus.BAD_REQUEST,
                messageForPath(request, "Dữ liệu không hợp lệ", "Validation failed"), request.getRequestURI(), details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException ex,
                                                                   HttpServletRequest request) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST,
                messageForPath(request, "Dữ liệu không hợp lệ", "Malformed request body"),
                request.getRequestURI(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN,
                messageForPath(request, "Forbidden", "Access denied"), request.getRequestURI(), null);
    }

    @ExceptionHandler(TopicServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleTopicUnavailable(TopicServiceUnavailableException ex,
                                                                  HttpServletRequest request) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception processing request: {}", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                messageForPath(request, "Lỗi hệ thống không xác định", "An unexpected internal error occurred"),
                request.getRequestURI(), null);
    }

    private String messageForPath(HttpServletRequest request, String personalLibraryMessage, String catalogMessage) {
        return request.getRequestURI().startsWith("/api/learning-support/")
                ? personalLibraryMessage : catalogMessage;
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, String path, Map<String, String> details) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                details
        );
        return ResponseEntity.status(status).body(response);
    }
}
