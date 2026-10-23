package com.ieltspath.content.api.exception;

import com.ieltspath.content.domain.exception.*;
import com.ieltspath.content.application.exception.VocabularyServiceUnavailableException;
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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
            TopicNotFoundException.class,
            CourseNotFoundException.class,
            KnowledgePointNotFoundException.class,
            ErrorTypeNotFoundException.class,
            ContentPackageNotFoundException.class,
            QuestionNotFoundException.class,
            QuestionVersionNotFoundException.class,
            AssetNotFoundException.class,
            ReadingPassageNotFoundException.class,
            LessonNotFoundException.class,
            LessonResourceNotFoundException.class,
            PackageVersionNotFoundException.class,
            ContentSectionNotFoundException.class,
            AiDraftNotFoundException.class,
            RubricNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFoundException(ContentDomainException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler({
            DuplicateCodeException.class,
            InvalidContentStateException.class,
            InvalidErrorTypeException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequestException(Exception ex, HttpServletRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(TopicSortOrderTakenException.class)
    public ResponseEntity<ErrorResponse> handleTopicSortOrderTaken(TopicSortOrderTakenException ex,
                                                                   HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "SORT_ORDER_TAKEN"));
    }

    @ExceptionHandler(AiQuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleAiQuotaExceeded(AiQuotaExceededException ex,
                                                               HttpServletRequest request) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "AI_QUOTA_EXCEEDED"));
    }

    @ExceptionHandler(DuplicateImportRefException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateImportRef(DuplicateImportRefException ex,
                                                                  HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "IMPORT_REF_CONFLICT"));
    }

    @ExceptionHandler(DraftVersionExistsException.class)
    public ResponseEntity<ErrorResponse> handleDraftVersionExists(DraftVersionExistsException ex,
                                                                  HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "DRAFT_EXISTS"));
    }

    @ExceptionHandler(VersionNotDraftException.class)
    public ResponseEntity<ErrorResponse> handleVersionNotDraft(VersionNotDraftException ex,
                                                               HttpServletRequest request) {
        log.warn("Version change refused: version is not a draft");
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "VERSION_NOT_DRAFT"));
    }

    @ExceptionHandler(UnknownErrorCodeException.class)
    public ResponseEntity<ErrorResponse> handleUnknownErrorCode(UnknownErrorCodeException ex,
                                                                HttpServletRequest request) {
        log.warn("Unknown error code refused: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "UNKNOWN_ERROR_CODE"));
    }

    @ExceptionHandler(InvalidPackageLessonException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPackageLesson(InvalidPackageLessonException ex,
                                                                    HttpServletRequest request) {
        log.warn("Package lesson refused: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "INVALID_PACKAGE_LESSON"));
    }

    @ExceptionHandler(QuestionAlreadyUsedException.class)
    public ResponseEntity<ErrorResponse> handleQuestionAlreadyUsed(QuestionAlreadyUsedException ex,
                                                                  HttpServletRequest request) {
        log.warn("Package question usage refused: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "QUESTION_ALREADY_USED"));
    }

    @ExceptionHandler(QuestionPurposeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleQuestionPurposeMismatch(QuestionPurposeMismatchException ex,
                                                                      HttpServletRequest request) {
        log.warn("Package question purpose refused: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "QUESTION_PURPOSE_MISMATCH"));
    }

    @ExceptionHandler(TopicSkillLockedException.class)
    public ResponseEntity<ErrorResponse> handleTopicSkillLocked(TopicSkillLockedException ex, HttpServletRequest request) {
        log.warn("Topic skill change refused: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "TOPIC_SKILL_LOCKED"));
    }

    @ExceptionHandler(RubricNotEditableException.class)
    public ResponseEntity<ErrorResponse> handleRubricNotEditable(RubricNotEditableException ex,
                                                                  HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "RUBRIC_NOT_EDITABLE"));
    }

    @ExceptionHandler(DuplicateCourseException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateCourse(DuplicateCourseException ex, HttpServletRequest request) {
        log.warn("Course conflict: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(DuplicateLessonResourceOrderException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateLessonResourceOrder(DuplicateLessonResourceOrderException ex,
                                                                           HttpServletRequest request) {
        log.warn("Lesson resource conflict: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "DUPLICATE_RESOURCE_ORDER"));
    }

    @ExceptionHandler(DifficultyRequiredException.class)
    public ResponseEntity<ErrorResponse> handleDifficultyRequired(DifficultyRequiredException ex,
                                                                    HttpServletRequest request) {
        log.warn("Question version refused: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "DIFFICULTY_REQUIRED"));
    }

    @ExceptionHandler(QuestionDifficultyRequiredForPublicationException.class)
    public ResponseEntity<ErrorResponse> handleQuestionDifficultyRequiredForPublication(
            QuestionDifficultyRequiredForPublicationException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "DIFFICULTY_REQUIRED"));
    }

    @ExceptionHandler(QuestionVersionNotPublishableException.class)
    public ResponseEntity<ErrorResponse> handleQuestionVersionNotPublishable(QuestionVersionNotPublishableException ex,
                                                                              HttpServletRequest request) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "QUESTION_VERSION_NOT_PUBLISHABLE"));
    }

    @ExceptionHandler(QuestionVersionStateException.class)
    public ResponseEntity<ErrorResponse> handleQuestionVersionState(QuestionVersionStateException ex,
                                                                     HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "QUESTION_VERSION_STATE_INVALID"));
    }

    @ExceptionHandler(QuestionVersionInUseException.class)
    public ResponseEntity<ErrorResponse> handleQuestionVersionInUse(QuestionVersionInUseException ex,
                                                                     HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "QUESTION_VERSION_IN_USE"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.put(error.getField(), error.getDefaultMessage());
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), details);
    }

    @ExceptionHandler(PrerequisiteCycleException.class)
    public ResponseEntity<ErrorResponse> handlePrerequisiteCycle(PrerequisiteCycleException ex,
                                                                 HttpServletRequest request) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "PREREQUISITE_CYCLE", "cycle", ex.getCycle()));
    }

    @ExceptionHandler(InvalidPrerequisiteException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPrerequisites(InvalidPrerequisiteException ex,
                                                                     HttpServletRequest request) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "PREREQUISITE_INVALID", "invalidIds", ex.getInvalidIds()));
    }

    @ExceptionHandler(PrerequisiteLevelException.class)
    public ResponseEntity<ErrorResponse> handlePrerequisiteLevel(PrerequisiteLevelException ex,
                                                                 HttpServletRequest request) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "PREREQUISITE_LEVEL_INVALID", "invalidIds", ex.getInvalidIds()));
    }

    @ExceptionHandler(KnowledgePointInUseException.class)
    public ResponseEntity<ErrorResponse> handleKnowledgePointInUse(KnowledgePointInUseException ex,
                                                                    HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "KNOWLEDGE_POINT_IN_USE"));
    }

    @ExceptionHandler(InvalidKnowledgePointCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidKnowledgePointCode(InvalidKnowledgePointCodeException ex,
                                                                          HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "INVALID_KNOWLEDGE_POINT_CODE"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException ex,
                                                                   HttpServletRequest request) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed request body", request.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleInvalidParameter(MethodArgumentTypeMismatchException ex,
                                                               HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid value for request parameter " + ex.getName(),
                request.getRequestURI(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI(), null);
    }

    @ExceptionHandler(InvalidLessonBlockException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLessonBlock(InvalidLessonBlockException ex, HttpServletRequest request) {
        log.error("Lesson data breaks a block rule at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "INVALID_LESSON_BLOCK"));
    }

    @ExceptionHandler(LessonBlockInvalidException.class)
    public ResponseEntity<ErrorResponse> handleLessonBlockInvalid(LessonBlockInvalidException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "LESSON_BLOCK_INVALID"));
    }

    @ExceptionHandler(LessonExerciseLockedException.class)
    public ResponseEntity<ErrorResponse> handleLessonExerciseLocked(LessonExerciseLockedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "LESSON_EXERCISE_LOCKED"));
    }

    @ExceptionHandler(LessonSortConflictException.class)
    public ResponseEntity<ErrorResponse> handleLessonSortConflict(LessonSortConflictException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "LESSON_SORT_CONFLICT"));
    }

    @ExceptionHandler(LessonNotPublishableException.class)
    public ResponseEntity<ErrorResponse> handleLessonNotPublishable(LessonNotPublishableException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "LESSON_NOT_PUBLISHABLE", "issues", ex.getIssues()));
    }

    @ExceptionHandler(VocabularyServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleVocabularyUnavailable(VocabularyServiceUnavailableException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "VOCABULARY_SERVICE_UNAVAILABLE"));
    }

    @ExceptionHandler(InvalidMediaReferenceException.class)
    public ResponseEntity<ErrorResponse> handleInvalidMediaReference(InvalidMediaReferenceException ex, HttpServletRequest request) {
        log.error("Stored media reference rejected at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI(),
                Map.of("code", "INVALID_MEDIA_REFERENCE"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception processing request: {}", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred", request.getRequestURI(), null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, String path,
                                                        Map<String, Object> details) {
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

