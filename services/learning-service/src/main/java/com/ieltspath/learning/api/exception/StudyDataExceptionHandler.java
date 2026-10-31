package com.ieltspath.learning.api.exception;

import com.ieltspath.learning.api.controller.AdminLearningGoalController;
import com.ieltspath.learning.api.controller.LearningActivityController;
import com.ieltspath.learning.api.controller.LearningGoalController;
import com.ieltspath.learning.api.controller.StudySessionController;
import com.ieltspath.learning.domain.exception.ConflictException;
import com.ieltspath.learning.domain.exception.InvalidDataException;
import com.ieltspath.learning.domain.exception.LearningGoalConflictException;
import com.ieltspath.learning.domain.exception.LearningGoalNotFoundException;
import com.ieltspath.learning.domain.exception.ResourceNotFoundException;
import com.ieltspath.learning.domain.exception.StudyTrackingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Errors of the goal, learning activity and study time endpoints, in the shape these endpoints had when the User Service
 * served them: Vietnamese messages, 400 for bad input, and a {@code code} in {@code details} for study tracking. It only
 * covers those controllers; the rest of Learning keeps {@link GlobalExceptionHandler}.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {LearningGoalController.class, AdminLearningGoalController.class,
        LearningActivityController.class, StudySessionController.class})
public class StudyDataExceptionHandler {
    private static final String INVALID = "Dữ liệu không hợp lệ";

    @ExceptionHandler(StudyTrackingException.class)
    ResponseEntity<ErrorResponse> studyTracking(StudyTrackingException exception, HttpServletRequest request) {
        HttpStatus status = switch (exception.getCode()) {
            case "SESSION_NOT_FOUND", "SOURCE_NOT_AVAILABLE" -> HttpStatus.NOT_FOUND;
            case "SOURCE_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.CONFLICT;
        };
        return error(status, exception.getCode(), request, Map.of("code", exception.getCode()));
    }

    @ExceptionHandler({ResourceNotFoundException.class, LearningGoalNotFoundException.class})
    ResponseEntity<ErrorResponse> notFound(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request, null);
    }

    @ExceptionHandler({LearningGoalConflictException.class, ConflictException.class})
    ResponseEntity<ErrorResponse> conflict(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request, null);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    ResponseEntity<ErrorResponse> badRequest(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, isActivity(request) ? INVALID : exception.getMessage(), request, null);
    }

    @ExceptionHandler({InvalidDataException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> invalidInput(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, INVALID, request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(fieldError -> details.put(fieldError.getField(), fieldError.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, INVALID, request, details);
    }

    private static boolean isActivity(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/learning/activities");
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message, HttpServletRequest request,
                                                Map<String, String> details) {
        return ResponseEntity.status(status).body(new ErrorResponse(LocalDateTime.now(), status.value(),
                isActivity(request) ? status.getReasonPhrase() : reasonPhrase(status), message,
                request.getRequestURI(), details));
    }

    private static String reasonPhrase(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Yêu cầu không hợp lệ";
            case NOT_FOUND -> "Không tìm thấy";
            case CONFLICT -> "Xung đột dữ liệu";
            case SERVICE_UNAVAILABLE -> "Dịch vụ tạm thời không khả dụng";
            default -> status.getReasonPhrase();
        };
    }
}
