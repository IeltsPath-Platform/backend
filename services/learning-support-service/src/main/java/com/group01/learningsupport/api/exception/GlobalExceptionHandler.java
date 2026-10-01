package com.group01.learningsupport.api.exception;

import com.group01.learningsupport.domain.exception.ConflictException;
import com.group01.learningsupport.domain.exception.InvalidDataException;
import com.group01.learningsupport.domain.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Không tìm thấy", request, null);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ErrorResponse> conflict(HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "Dữ liệu đã tồn tại", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> forbidden(HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "Forbidden", request, null);
    }

    @ExceptionHandler({
            InvalidDataException.class,
            IllegalArgumentException.class,
            IllegalStateException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<ErrorResponse> badRequest(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(fieldError -> details.put(fieldError.getField(), fieldError.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", request, details);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(HttpServletRequest request) {
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
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                details
        ));
    }
}
