package com.ieltspath.content.api.controller;

import com.ieltspath.content.application.usecase.ManageRubricsUseCase;
import com.ieltspath.content.domain.aggregate.Rubric;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/content")
public class RubricController {
    private final ManageRubricsUseCase rubrics;

    public RubricController(ManageRubricsUseCase rubrics) { this.rubrics = rubrics; }

    /** Examiner read surface exposes only the currently active public rubric. */
    @GetMapping("/rubrics")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHOR', 'EXAMINER')")
    public List<RubricResponse> active(@RequestParam("task") String task) {
        return rubrics.active(task).stream().map(RubricResponse::from).toList();
    }

    @GetMapping("/admin/rubrics")
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
    public List<RubricResponse> drafts(@RequestParam("task") String task) {
        return rubrics.drafts(task).stream().map(RubricResponse::from).toList();
    }

    @PostMapping("/admin/rubrics")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
    public RubricResponse create(@Valid @RequestBody RubricRequest request) {
        return RubricResponse.from(rubrics.create(request.code(), request.task(), request.version(), request.sourceNote(),
                request.criteria().stream().map(CriterionRequest::toDomain).toList(),
                request.examples().stream().map(ExampleRequest::toDomain).toList()));
    }

    @PutMapping("/admin/rubrics/{code}/{version}")
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
    public RubricResponse update(@PathVariable("code") String code, @PathVariable("version") int version,
                                 @Valid @RequestBody RubricRequest request) {
        if (!code.equals(request.code()) || version != request.version()) {
            throw new IllegalArgumentException("Rubric code and version in the body must match the path");
        }
        return RubricResponse.from(rubrics.updateDraft(code, version, request.task(), request.sourceNote(),
                request.criteria().stream().map(CriterionRequest::toDomain).toList(),
                request.examples().stream().map(ExampleRequest::toDomain).toList()));
    }

    @PostMapping("/admin/rubrics/{code}/{version}/activate")
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
    public RubricResponse activate(@PathVariable("code") String code, @PathVariable("version") int version) {
        return RubricResponse.from(rubrics.activate(code, version));
    }

    public record RubricRequest(@NotBlank String code, @NotBlank String task, @Min(1) int version,
                                @NotBlank String sourceNote, @NotEmpty List<@Valid CriterionRequest> criteria,
                                List<@Valid ExampleRequest> examples) {
        public RubricRequest {
            if (examples == null) examples = List.of();
        }
    }

    public record CriterionRequest(@NotBlank String code, @NotBlank String name, @NotBlank String description,
                                   @Min(1) int sortOrder, @NotEmpty List<@Valid BandDescriptorRequest> bandDescriptors) {
        Rubric.Criterion toDomain() {
            return new Rubric.Criterion(code, name, description, sortOrder,
                    bandDescriptors.stream().map(BandDescriptorRequest::toDomain).toList());
        }
    }

    public record BandDescriptorRequest(@DecimalMin("0.0") @DecimalMax("9.0") @NotNull Double band,
                                        @NotBlank String descriptor) {
        Rubric.BandDescriptor toDomain() { return new Rubric.BandDescriptor(band, descriptor); }
    }

    public record ExampleRequest(@DecimalMin("0.0") @DecimalMax("9.0") @NotNull Double band,
                                 @NotBlank String prompt, @NotBlank String essay, @NotBlank String commentary) {
        Rubric.Example toDomain() { return new Rubric.Example(band, prompt, essay, commentary); }
    }

    public record RubricResponse(String code, String task, int version, String status, String sourceNote,
                                  List<CriterionResponse> criteria, List<Rubric.Example> examples) {
        static RubricResponse from(Rubric rubric) {
            return new RubricResponse(rubric.code(), rubric.task(), rubric.version(), rubric.status(),
                    rubric.sourceNote(), rubric.criteria().stream().map(CriterionResponse::from).toList(),
                    rubric.examples());
        }
    }

    /** Public and authoring rubrics keep the domain fields they exposed before internal sub-KP mapping. */
    public record CriterionResponse(String code, String name, String description, int sortOrder,
                                    List<Rubric.BandDescriptor> bandDescriptors, java.util.UUID knowledgePointId) {
        static CriterionResponse from(Rubric.Criterion criterion) {
            return new CriterionResponse(criterion.code(), criterion.name(), criterion.description(),
                    criterion.sortOrder(), criterion.bandDescriptors(), criterion.knowledgePointId());
        }
    }
}
