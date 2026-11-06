package com.ieltspath.learning.api.exception;

import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.UploadRejectedException;
import com.ieltspath.events.messaging.InvalidDeadLetterBatchException;
import com.ieltspath.events.messaging.UnknownDeadLetterQueueException;
import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.exception.InsufficientPointsException;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.domain.exception.HeartsDepletedException;
import com.ieltspath.learning.domain.exception.LearningGateException;
import com.ieltspath.learning.domain.exception.PracticeAttemptAlreadySubmittedException;
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
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingRequestHeaderException;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UnknownDeadLetterQueueException.class)
    public ResponseEntity<LearningErrorResponse> unknownDeadLetterQueue(UnknownDeadLetterQueueException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new LearningErrorResponse(exception.getMessage(), "UNKNOWN_DEAD_LETTER_QUEUE", null));
    }

    @ExceptionHandler(InvalidDeadLetterBatchException.class)
    public ResponseEntity<LearningErrorResponse> invalidDeadLetterBatch(InvalidDeadLetterBatchException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new LearningErrorResponse(exception.getMessage(), "INVALID_DEAD_LETTER_BATCH", null));
    }
    @ExceptionHandler(PracticeAttemptAlreadySubmittedException.class)
    public ResponseEntity<LearningErrorResponse> submitted(PracticeAttemptAlreadySubmittedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new LearningErrorResponse(exception.getMessage(), "ATTEMPT_ALREADY_SUBMITTED", null));
    }
    @ExceptionHandler(HeartsDepletedException.class)
    public ResponseEntity<LearningErrorResponse> heartsDepleted(HeartsDepletedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new LearningErrorResponse(exception.getMessage(), "HEARTS_DEPLETED", null, null, null,
                        exception.getNextHeartAt(), exception.getRefillCost()));
    }

    /** A purchase with points (a heart refill, a streak freeze) that the balance cannot cover. */
    @ExceptionHandler(InsufficientPointsException.class)
    public ResponseEntity<LearningErrorResponse> insufficientPoints(InsufficientPointsException exception) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(new LearningErrorResponse("Not enough points", "INSUFFICIENT_POINTS", null));
    }

    /** Access could not be reached or refused the request; nothing was charged, so the purchase can be tried again. */
    @ExceptionHandler(AccessUnavailableException.class)
    public ResponseEntity<LearningErrorResponse> accessUnavailable(AccessUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new LearningErrorResponse("Points service is unavailable; try again later",
                        "ACCESS_UNAVAILABLE", null));
    }

    @ExceptionHandler(LearningGateException.class)
    public ResponseEntity<LearningErrorResponse> gate(LearningGateException exception) {
        var reviews = "REVIEW_REQUIRED".equals(exception.getCode()) ? exception.getReviews().stream()
                .map(review -> new LearningErrorResponse.Review(review.reviewId(), review.lessonId(),
                        review.knowledgePointId(), review.skill())).toList() : null;
        var lessonIds = "PRACTICE_REQUIRED".equals(exception.getCode()) ? exception.getLessonIds() : null;
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new LearningErrorResponse(exception.getMessage(), exception.getCode(), reviews, null, lessonIds));
    }

    @ExceptionHandler(LearningRequestException.class)
    public ResponseEntity<LearningErrorResponse> learningRequest(LearningRequestException exception) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(exception.getStatus());
        if ("AI_BUSY".equals(exception.getCode())) response.header("Retry-After", "5");
        if ("CONTENT_UNAVAILABLE".equals(exception.getCode())) response.header("Retry-After", "30");
        return response
                .body(new LearningErrorResponse(exception.getMessage(), exception.getCode(), null,
                        exception.getSubmissionId(), exception.getLessonIds()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<LearningErrorResponse> validation(MethodArgumentNotValidException exception,
                                                     HttpServletRequest request) {
        return ResponseEntity.unprocessableEntity().body(new LearningErrorResponse("Invalid request", null, null));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<LearningErrorResponse> missingHeader(MissingRequestHeaderException exception) {
        return ResponseEntity.badRequest().body(new LearningErrorResponse("Required header is missing",
                "INVALID_REQUEST", null));
    }

    /** A recording the bucket would refuse: empty, too large, a type that is not allowed, or content that is not what it claims. */
    @ExceptionHandler(UploadRejectedException.class)
    public ResponseEntity<LearningErrorResponse> recordingRejected(UploadRejectedException exception) {
        HttpStatus status = exception.reason() == UploadRejectedException.Reason.TOO_LARGE
                ? HttpStatus.PAYLOAD_TOO_LARGE : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(new LearningErrorResponse("The recording was refused",
                "RECORDING_" + exception.reason().name(), null));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<LearningErrorResponse> recordingTooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(new LearningErrorResponse(
                "The recording is too large", "RECORDING_TOO_LARGE", null));
    }

    /** The message of a storage failure holds no key or credential, but only its operation and reason are logged. */
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<LearningErrorResponse> storageUnavailable(StorageException exception) {
        log.error("Recording storage failed: operation={} reason={}", exception.operation(), exception.reason());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new LearningErrorResponse(
                "Recording storage is unavailable", "STORAGE_UNAVAILABLE", null));
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
        String path = request.getRequestURI();
        if ("/v3/api-docs".equals(path) || path.startsWith("/v3/api-docs/")) {
            log.error("OpenAPI generation failed: correlationId={}",
                    request.getHeader("X-Correlation-Id"), exception);
        } else {
            // The wrapper alone (a ServletException around an Error, say) says nothing. The class of the root cause and the
            // line it came from are logged, never its message, which may hold what the learner sent.
            Throwable root = exception;
            while (root.getCause() != null && root.getCause() != root) root = root.getCause();
            StackTraceElement[] frames = root.getStackTrace();
            log.error("Unexpected request failure: type={}, rootCause={}, at={}, correlationId={}",
                    exception.getClass().getName(), root.getClass().getName(), frames.length > 0 ? frames[0] : "-",
                    request.getHeader("X-Correlation-Id"));
        }
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred", request, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                HttpServletRequest request, Map<String, String> details) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI(), details));
    }
}
