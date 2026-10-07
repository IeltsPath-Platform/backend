package com.ieltspath.assessment.api.exception;

import com.ieltspath.assessment.application.exception.ContentUnavailableException;
import com.ieltspath.assessment.domain.exception.*;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AssessmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(AssessmentDomainException e, HttpServletRequest r) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), r.getRequestURI(), null);
    }

    @ExceptionHandler({InvalidAssessmentStateException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> bad(Exception e, HttpServletRequest r) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), r.getRequestURI(), null);
    }

    @ExceptionHandler(AttemptExpiredException.class)
    public ResponseEntity<ErrorResponse> expired(AttemptExpiredException e, HttpServletRequest r) {
        return build(HttpStatus.CONFLICT, e.getMessage(), r.getRequestURI(), null, "ATTEMPT_EXPIRED");
    }

    @ExceptionHandler(PackageNotAttemptableException.class)
    public ResponseEntity<ErrorResponse> notAttemptable(PackageNotAttemptableException e, HttpServletRequest r) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), r.getRequestURI(), null,
                "PACKAGE_NOT_ATTEMPTABLE");
    }

    @ExceptionHandler(ContentUnavailableException.class)
    public ResponseEntity<ErrorResponse> contentUnavailable(ContentUnavailableException e, HttpServletRequest r) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Content Service is unavailable", r.getRequestURI(), null,
                "CONTENT_UNAVAILABLE");
    }

    @ExceptionHandler({RevisionConflictException.class, OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> conflict(Exception e, HttpServletRequest r) {
        return build(HttpStatus.CONFLICT, "The assessment was changed by another request", r.getRequestURI(), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException e, HttpServletRequest r) {
        return build(HttpStatus.CONFLICT, "The assessment request conflicts with existing data", r.getRequestURI(), null);
    }

    @ExceptionHandler(AssessmentAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> denied(AssessmentAccessDeniedException e, HttpServletRequest r) {
        return build(HttpStatus.FORBIDDEN, "Access denied", r.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e, HttpServletRequest r) {
        Map<String, String> details = new HashMap<>();
        for (FieldError f : e.getBindingResult().getFieldErrors()) details.put(f.getField(), f.getDefaultMessage());
        return build(HttpStatus.BAD_REQUEST, "Validation failed", r.getRequestURI(), details);
    }

    @ExceptionHandler(org.springframework.web.client.RestClientException.class)
    public ResponseEntity<ErrorResponse> dependencyUnavailable(org.springframework.web.client.RestClientException e, HttpServletRequest r) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "A required service is unavailable", r.getRequestURI(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> securityDenied(AccessDeniedException e, HttpServletRequest r) {
        return build(HttpStatus.FORBIDDEN, "Access denied", r.getRequestURI(), null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> methodNotAllowed(HttpRequestMethodNotSupportedException e, HttpServletRequest r) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", r.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> generic(Exception e, HttpServletRequest r) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred", r.getRequestURI(), null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus s, String m, String p, Map<String, String> d) {
        return build(s, m, p, d, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus s, String m, String p, Map<String, String> d, String code) {
        return ResponseEntity.status(s)
                .body(new ErrorResponse(LocalDateTime.now(), s.value(), s.getReasonPhrase(), m, p, d, code));
    }
}
