package com.ieltspath.learning.api.exception;

import com.ieltspath.commonsecurity.error.VietnameseErrorMessages;
import com.ieltspath.learning.api.controller.AdminLearningGoalController;
import com.ieltspath.learning.api.controller.LearningActivityController;
import com.ieltspath.learning.api.controller.LearningGoalController;
import com.ieltspath.learning.api.controller.StudySessionController;
import com.ieltspath.learning.api.controller.PracticeSessionController;
import com.ieltspath.learning.domain.exception.ConflictException;
import com.ieltspath.learning.domain.exception.InvalidDataException;
import com.ieltspath.learning.domain.exception.LearningGoalConflictException;
import com.ieltspath.learning.domain.exception.LearningGoalNotFoundException;
import com.ieltspath.learning.domain.exception.ResourceNotFoundException;
import com.ieltspath.learning.domain.exception.StudyTrackingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {LearningGoalController.class, AdminLearningGoalController.class,
        LearningActivityController.class, StudySessionController.class, PracticeSessionController.class})
public class StudyDataExceptionHandler {
    private static final String INVALID = "Dữ liệu không hợp lệ";

    @ExceptionHandler(StudyTrackingException.class)
    ResponseEntity<ErrorResponse> studyTracking(StudyTrackingException exception, HttpServletRequest request) {
        HttpStatus status = switch (exception.getCode()) {
            case "SESSION_NOT_FOUND", "SOURCE_NOT_AVAILABLE" -> HttpStatus.NOT_FOUND;
            case "SOURCE_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.CONFLICT;
        };
        String message = switch (exception.getCode()) {
            case "SESSION_NOT_FOUND" -> "Không tìm thấy phiên học. Vui lòng bắt đầu phiên học mới.";
            case "SOURCE_NOT_AVAILABLE" -> "Nguồn học hiện không khả dụng hoặc không tồn tại.";
            case "SOURCE_UNAVAILABLE" -> "Không thể xác minh nguồn học lúc này. Vui lòng thử lại sau.";
            case "SESSION_IN_USE" -> "Phiên học đang hoạt động trên thiết bị hoặc tab khác.";
            case "SESSION_CLOSED" -> "Phiên học đã kết thúc. Vui lòng mở phiên học mới.";
            case "LEASE_EXPIRED" -> "Phiên học đã hết hạn giữ chỗ. Vui lòng mở lại phiên học.";
            case "STALE_EPOCH" -> "Phiên học đã được làm mới ở nơi khác. Vui lòng tải lại.";
            case "INVALID_SEQUENCE" -> "Thứ tự nhịp học không hợp lệ. Vui lòng đồng bộ lại phiên học.";
            case "PAYLOAD_CONFLICT" -> "Yêu cầu xung đột với dữ liệu phiên học đã ghi nhận.";
            default -> VietnameseErrorMessages.generic(status.value());
        };
        return error(status, message, request, Map.of("code", exception.getCode()));
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
        String userMessage = VietnameseErrorMessages.userFacing(status.value(), message);
        if (!userMessage.equals(message)) {
            log.warn("Replaced non-Vietnamese error message (status={}): {}", status.value(), message);
        }
        return ResponseEntity.status(status).body(new ErrorResponse(LocalDateTime.now(), status.value(),
                isActivity(request) ? status.getReasonPhrase() : reasonPhrase(status), userMessage,
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
