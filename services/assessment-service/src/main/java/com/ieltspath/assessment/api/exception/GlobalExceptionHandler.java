package com.ieltspath.assessment.api.exception;

import com.ieltspath.assessment.application.exception.ContentUnavailableException;
import com.ieltspath.assessment.application.exception.UnknownErrorCodeException;
import com.ieltspath.assessment.application.exception.UserServiceUnavailableException;
import com.ieltspath.assessment.domain.exception.*;
import com.ieltspath.commonstorage.StorageException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponse> storageUnavailable(StorageException e, HttpServletRequest r) {
        log.error("Speaking recording storage failed: operation={} reason={}", e.operation(), e.reason());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Recording storage is unavailable", r.getRequestURI(), null,
                "STORAGE_UNAVAILABLE");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> uploadTooLarge(MaxUploadSizeExceededException e, HttpServletRequest r) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Recording file is too large", r.getRequestURI(), null,
                "UPLOAD_TOO_LARGE");
    }

    @ExceptionHandler(JobAlreadyClaimedException.class)
    public ResponseEntity<ErrorResponse> claimed(JobAlreadyClaimedException e, HttpServletRequest r) {
        return build(HttpStatus.CONFLICT, e.getMessage(), r.getRequestURI(), null, "JOB_ALREADY_CLAIMED");
    }
    @ExceptionHandler(ExaminerUnavailableException.class)
    public ResponseEntity<ErrorResponse> examinerUnavailable(ExaminerUnavailableException e, HttpServletRequest r) {
        return build(HttpStatus.CONFLICT, e.getMessage(), r.getRequestURI(), Map.of("reason", e.reason().name()),
                "EXAMINER_UNAVAILABLE");
    }

    @ExceptionHandler(ExaminerChangeNotAllowedException.class)
    public ResponseEntity<ErrorResponse> examinerChangeNotAllowed(ExaminerChangeNotAllowedException e, HttpServletRequest r) {
        return build(HttpStatus.CONFLICT, e.getMessage(), r.getRequestURI(), Map.of("reason", e.reason().name()),
                "EXAMINER_CHANGE_NOT_ALLOWED");
    }

    @ExceptionHandler(GradingCancelNotAllowedException.class)
    public ResponseEntity<ErrorResponse> gradingCancelNotAllowed(GradingCancelNotAllowedException e, HttpServletRequest r) {
        return build(HttpStatus.CONFLICT, e.getMessage(), r.getRequestURI(), Map.of("reason", e.reason().name()),
                "GRADING_CANCEL_NOT_ALLOWED");
    }

    @ExceptionHandler(UserServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> userServiceUnavailable(UserServiceUnavailableException e, HttpServletRequest r) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "User Service is unavailable", r.getRequestURI(), null,
                "USER_SERVICE_UNAVAILABLE");
    }

    @ExceptionHandler(HumanGradingCreditInsufficientException.class)
    public ResponseEntity<ErrorResponse> creditsRequired(HumanGradingCreditInsufficientException e, HttpServletRequest r) {
        return build(HttpStatus.PAYMENT_REQUIRED, e.getMessage(), r.getRequestURI(), null, "HUMAN_GRADING_CREDIT_REQUIRED");
    }
    @ExceptionHandler(HumanGradingCreditConflictException.class)
    public ResponseEntity<ErrorResponse> creditConflict(HumanGradingCreditConflictException e, HttpServletRequest r) {
        return build(HttpStatus.CONFLICT, e.getMessage(), r.getRequestURI(), null, "HUMAN_GRADING_CREDIT_CONFLICT");
    }
    @ExceptionHandler(AssessmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(AssessmentDomainException e, HttpServletRequest r) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), r.getRequestURI(), null);
    }

    @ExceptionHandler({InvalidAssessmentStateException.class, IllegalArgumentException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.method.annotation.HandlerMethodValidationException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
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

    @ExceptionHandler(UnknownErrorCodeException.class)
    public ResponseEntity<ErrorResponse> unknownErrorCode(UnknownErrorCodeException e, HttpServletRequest r) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), r.getRequestURI(), null, "UNKNOWN_ERROR_CODE");
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

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> responseStatus(ResponseStatusException e, HttpServletRequest r) {
        HttpStatusCode status = e.getStatusCode();
        HttpStatus knownStatus = HttpStatus.resolve(status.value());
        String error = knownStatus == null ? "Request failed" : knownStatus.getReasonPhrase();
        String reason = e.getReason();
        return ResponseEntity.status(status).body(new ErrorResponse(LocalDateTime.now(), status.value(), error,
                reason == null ? error : reason, r.getRequestURI(), null, reason));
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
