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
            LearningGoalConflictException.class, ConflictException.class})
    ResponseEntity<ErrorResponse> handleConflict(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request, null);
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
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> handleMalformedActivityRequest(Exception exception, HttpServletRequest request) {
        return isLearningSupportPath(request)
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
