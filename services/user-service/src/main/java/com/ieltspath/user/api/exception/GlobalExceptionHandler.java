package com.ieltspath.user.api.exception;

import com.ieltspath.user.domain.exception.AuthenticationFailedException;
import com.ieltspath.user.domain.exception.ConflictException;
import com.ieltspath.user.domain.exception.EmailAlreadyExistsException;
import com.ieltspath.user.domain.exception.InvalidActionTokenException;
import com.ieltspath.user.domain.exception.InvalidDataException;
import com.ieltspath.user.domain.exception.InvalidUserStatusException;
import com.ieltspath.user.domain.exception.LearnerProfileNotFoundException;
import com.ieltspath.user.domain.exception.LearningGoalNotFoundException;
import com.ieltspath.user.domain.exception.LearningGoalConflictException;
import com.ieltspath.user.domain.exception.OAuthIdentityNotFoundException;
import com.ieltspath.user.domain.exception.PhoneAlreadyExistsException;
import com.ieltspath.user.domain.exception.RoleNotFoundException;
import com.ieltspath.user.domain.exception.ResourceNotFoundException;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.exception.UserManagementRuleViolationException;
import com.ieltspath.user.domain.exception.PasswordChangeException;
import com.ieltspath.user.domain.exception.PasswordNotConfiguredException;
import com.ieltspath.user.application.exception.AvatarUploadRateLimitException;
import com.ieltspath.commonstorage.UploadRejectedException;
import com.ieltspath.commonstorage.StorageException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(com.ieltspath.user.domain.exception.StudyTrackingException.class)
    ResponseEntity<ErrorResponse> handleStudyTracking(com.ieltspath.user.domain.exception.StudyTrackingException exception,
                                                     HttpServletRequest request) {
        HttpStatus status = switch(exception.getCode()) {
            case "SESSION_NOT_FOUND", "SOURCE_NOT_AVAILABLE" -> HttpStatus.NOT_FOUND;
            case "SOURCE_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.CONFLICT;
        };
        return error(status, exception.getCode(), request, Map.of("code", exception.getCode()));
    }
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    void handleClientAbort(AsyncRequestNotUsableException exception, HttpServletRequest request) {
        log.debug("Client disconnected before response completed path={} reason={}",
                request.getRequestURI(), exception.getMessage());
    }

    @ExceptionHandler({
            UserNotFoundException.class,
            ResourceNotFoundException.class,
            LearnerProfileNotFoundException.class,
            LearningGoalNotFoundException.class,
            OAuthIdentityNotFoundException.class
    })
    ResponseEntity<ErrorResponse> handleNotFound(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request, null);
    }

    @ExceptionHandler({EmailAlreadyExistsException.class, PhoneAlreadyExistsException.class,
            LearningGoalConflictException.class, ConflictException.class, PasswordNotConfiguredException.class})
    ResponseEntity<ErrorResponse> handleConflict(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request, null);
    }

    @ExceptionHandler(PasswordChangeException.class)
    ResponseEntity<ErrorResponse> handlePasswordChange(PasswordChangeException exception, HttpServletRequest request) {
        HttpStatus status = exception.isRateLimited() ? HttpStatus.TOO_MANY_REQUESTS
                : "CURRENT_PASSWORD_INVALID".equals(exception.getCode()) ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
        return error(status, exception.getMessage(), request, Map.of("code", exception.getCode()));
    }

    @ExceptionHandler(UploadRejectedException.class)
    ResponseEntity<ErrorResponse> handleUploadRejected(UploadRejectedException exception, HttpServletRequest request) {
        HttpStatus status = switch (exception.reason()) {
            case TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE;
            case TYPE_NOT_ALLOWED -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            default -> HttpStatus.BAD_REQUEST;
        };
        String message = switch (exception.reason()) {
            case EMPTY -> "Chưa chọn ảnh đại diện";
            case TOO_LARGE -> "Ảnh đại diện không được vượt quá 2 MB";
            case TYPE_NOT_ALLOWED -> "Chỉ chấp nhận ảnh JPEG, PNG hoặc WebP";
            case CONTENT_MISMATCH -> "Nội dung ảnh không khớp với định dạng đã khai báo";
        };
        return error(status, message, request, null);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> handleMaxUploadSize(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh đại diện không được vượt quá 2 MB", request, null);
    }

    @ExceptionHandler(StorageException.class)
    ResponseEntity<ErrorResponse> handleStorage(StorageException exception, HttpServletRequest request) {
        log.error("Avatar storage failed operation={} reason={} ref={}", exception.operation(), exception.reason(), exception.ref());
        return error(HttpStatus.SERVICE_UNAVAILABLE, "Kho ảnh tạm thời không khả dụng", request, null);
    }

    @ExceptionHandler(AvatarUploadRateLimitException.class)
    ResponseEntity<ErrorResponse> handleAvatarRateLimit(AvatarUploadRateLimitException exception, HttpServletRequest request) {
        return error(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage(), request, null);
    }

    @ExceptionHandler(UserManagementRuleViolationException.class)
    ResponseEntity<ErrorResponse> handleUserManagementRule(UserManagementRuleViolationException exception,
                                                           HttpServletRequest request) {
        HttpStatus status = "ROLE_CONFLICT".equals(exception.getCode())
                ? HttpStatus.UNPROCESSABLE_ENTITY
                : HttpStatus.CONFLICT;
        return error(status, exception.getMessage(), request, Map.of("code", exception.getCode()));
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    ResponseEntity<ErrorResponse> handleAuthenticationFailed(AuthenticationFailedException exception, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, exception.getMessage(), request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "Forbidden", request, null);
    }

    @ExceptionHandler({
            RoleNotFoundException.class,
            InvalidUserStatusException.class,
            InvalidActionTokenException.class,
            IllegalArgumentException.class,
            IllegalStateException.class
    })
    ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST,
                isLearningSupportPath(request) ? "Dữ liệu không hợp lệ" : exception.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidDataException.class)
    ResponseEntity<ErrorResponse> handleInvalidActivityData(InvalidDataException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", request, null);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> handleMalformedActivityRequest(Exception exception, HttpServletRequest request) {
        return isLearningSupportPath(request) || request.getRequestURI().startsWith("/api/users/me/study-sessions")
                || request.getRequestURI().equals("/api/users/me/avatar")
                || request.getRequestURI().startsWith("/api/users/me/statistics/")
                || request.getRequestURI().equals("/internal/users/public-profiles")
                || request.getRequestURI().matches("/api/users/[^/]+/public-profile")
                ? error(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", request, null)
                : error(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống không xác định", request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                details.put(fieldError.getField(), fieldError.getDefaultMessage())
        );
        return error(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", request, details);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected exception", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống không xác định", request, null);
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> details
    ) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                isLearningSupportPath(request) ? status.getReasonPhrase() : reasonPhrase(status),
                message,
                request.getRequestURI(),
                details
        ));
    }

    private boolean isLearningSupportPath(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/learning-support/");
    }

    private String reasonPhrase(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Yêu cầu không hợp lệ";
            case UNAUTHORIZED -> "Chưa xác thực";
            case NOT_FOUND -> "Không tìm thấy";
            case CONFLICT -> "Xung đột dữ liệu";
            case SERVICE_UNAVAILABLE -> "Dịch vụ tạm thời không khả dụng";
            case INTERNAL_SERVER_ERROR -> "Lỗi hệ thống";
            default -> status.getReasonPhrase();
        };
    }
}
