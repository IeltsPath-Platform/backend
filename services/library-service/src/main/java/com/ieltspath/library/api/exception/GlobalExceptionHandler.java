package com.ieltspath.library.api.exception;

import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.UploadRejectedException;
import com.ieltspath.library.application.exception.TopicServiceUnavailableException;
import com.ieltspath.library.domain.exception.ConflictException;
import com.ieltspath.library.domain.exception.ContentDomainException;
import com.ieltspath.library.domain.exception.DuplicateCodeException;
import com.ieltspath.library.domain.exception.InvalidContentStateException;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.exception.ResourceNotFoundException;
import com.ieltspath.library.domain.exception.VideoNotFoundException;
import com.ieltspath.library.domain.exception.VocabularyNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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

    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDataException(InvalidDataException ex,
                                                                   HttpServletRequest request) {
        String message = ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Dữ liệu không hợp lệ";
        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), null);
    }

    @ExceptionHandler({IllegalStateException.class,
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

    @ExceptionHandler(UploadRejectedException.class)
    public ResponseEntity<ErrorResponse> handleUploadRejected(UploadRejectedException ex,
                                                               HttpServletRequest request) {
        HttpStatus status = ex.reason() == UploadRejectedException.Reason.TOO_LARGE
                ? HttpStatus.PAYLOAD_TOO_LARGE
                : HttpStatus.BAD_REQUEST;
        String message = switch (ex.reason()) {
            case EMPTY -> "Tệp tải lên không được để trống";
            case TOO_LARGE -> "Kích thước tệp vượt quá giới hạn cho phép";
            case TYPE_NOT_ALLOWED, CONTENT_MISMATCH -> "Định dạng tệp không được hỗ trợ";
        };
        return buildResponse(status, message, request.getRequestURI(), null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex,
                                                                     HttpServletRequest request) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "Kích thước tệp vượt quá giới hạn cho phép", request.getRequestURI(), null);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponse> handleStorageException(StorageException ex,
                                                                HttpServletRequest request) {
        log.error("Lỗi thao tác lưu trữ: operation={} reason={}", ex.operation(), ex.reason());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ lưu trữ hiện không khả dụng", request.getRequestURI(), null);
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

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ResponseEntity<ErrorResponse> handleMethodValidation(Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                messageForPath(request, "Dữ liệu không hợp lệ", "Validation failed"), request.getRequestURI(), null);
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
