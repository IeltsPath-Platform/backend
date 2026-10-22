package com.ieltspath.content.api.controller;

import com.ieltspath.content.application.usecase.CreateErrorTypeUseCase;
import com.ieltspath.content.application.usecase.ListErrorTypesUseCase;
import com.ieltspath.content.application.usecase.UpdateErrorTypeUseCase;
import com.ieltspath.content.domain.aggregate.ErrorType;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.ErrorCategory;
import com.ieltspath.content.domain.vo.ErrorReviewStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/content/error-types")
@RequiredArgsConstructor
public class ErrorTypeController {
    private final ListErrorTypesUseCase listErrorTypes;
    private final CreateErrorTypeUseCase createErrorType;
    private final UpdateErrorTypeUseCase updateErrorType;

    @GetMapping
    @PreAuthorize("hasAnyRole('CONTENT_AUTHOR', 'EXAMINER')")
    public List<ErrorTypeResponse> list(@RequestParam(value = "activeOnly", defaultValue = "false") boolean activeOnly) {
        return listErrorTypes.execute(activeOnly).stream().map(ErrorTypeResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
    public ErrorTypeResponse create(@Valid @RequestBody CreateErrorTypeRequest request) {
        return ErrorTypeResponse.from(createErrorType.execute(request.code(), request.name(), request.category(),
                request.severity(), request.knowledgePointId(), request.fixHint()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
    public ErrorTypeResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateErrorTypeRequest request) {
        return ErrorTypeResponse.from(updateErrorType.execute(id, request.name(), request.category(),
                request.severity(), request.knowledgePointId(), request.fixHint(), request.status(),
                request.reviewStatus()));
    }

    public record CreateErrorTypeRequest(
            @NotBlank String code,
            @NotBlank String name,
            @NotNull ErrorCategory category,
            @Min(1) @Max(3) int severity,
            UUID knowledgePointId,
            @NotBlank String fixHint) {}

    /** The code is fixed at creation and is not accepted here. */
    public record UpdateErrorTypeRequest(
            @NotBlank String name,
            @NotNull ErrorCategory category,
            @Min(1) @Max(3) int severity,
            UUID knowledgePointId,
            @NotBlank String fixHint,
            ContentStatus status,
            ErrorReviewStatus reviewStatus) {}

    public record ErrorTypeResponse(UUID id, String code, String name, ErrorCategory category, int severity,
                                    UUID knowledgePointId, String fixHint, ContentStatus status,
                                    ErrorReviewStatus reviewStatus) {
        static ErrorTypeResponse from(ErrorType errorType) {
            return new ErrorTypeResponse(errorType.getId(), errorType.getCode(), errorType.getName(),
                    errorType.getCategory(), errorType.getSeverity(), errorType.getKnowledgePointId(),
                    errorType.getFixHint(), errorType.getStatus(), errorType.getReviewStatus());
        }
    }
}
