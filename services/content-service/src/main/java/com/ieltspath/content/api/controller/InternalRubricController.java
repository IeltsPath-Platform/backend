package com.ieltspath.content.api.controller;

import com.ieltspath.content.application.usecase.ManageRubricsUseCase;
import com.ieltspath.content.domain.aggregate.Rubric;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Internal snapshot endpoints used by Learning and Assessment essay graders. */
@RestController
@RequestMapping("/internal/learning-content/rubrics")
public class InternalRubricController {
    private final ManageRubricsUseCase rubrics;

    public InternalRubricController(ManageRubricsUseCase rubrics) { this.rubrics = rubrics; }

    @GetMapping("/active")
    public RubricPromptResponse active(@RequestParam("task") String task) {
        return RubricPromptResponse.from(rubrics.activeRubric(task));
    }

    @GetMapping("/{code}/{version}")
    public RubricPromptResponse version(@PathVariable("code") String code, @PathVariable("version") int version) {
        return RubricPromptResponse.from(rubrics.byVersion(code, version));
    }

    /** This shape is shared with the Learning and Assessment grading clients. */
    public record RubricPromptResponse(String code, String task, int version, String sourceNote,
                                       List<CriterionResponse> criteria, List<ExampleResponse> examples) {
        static RubricPromptResponse from(Rubric rubric) {
            return new RubricPromptResponse(rubric.code(), rubric.task(), rubric.version(), rubric.sourceNote(),
                    rubric.criteria().stream().map(CriterionResponse::from).toList(),
                    rubric.examples().stream().map(ExampleResponse::from).toList());
        }
    }

    public record CriterionResponse(String code, String name, String description, int sortOrder,
                                    List<BandDescriptorResponse> bandDescriptors) {
        static CriterionResponse from(Rubric.Criterion criterion) {
            return new CriterionResponse(criterion.code(), criterion.name(), criterion.description(), criterion.sortOrder(),
                    criterion.bandDescriptors().stream().map(BandDescriptorResponse::from).toList());
        }
    }

    public record BandDescriptorResponse(double band, String descriptor) {
        static BandDescriptorResponse from(Rubric.BandDescriptor descriptor) {
            return new BandDescriptorResponse(descriptor.band(), descriptor.descriptor());
        }
    }

    public record ExampleResponse(double band, String prompt, String essay, String commentary) {
        static ExampleResponse from(Rubric.Example example) {
            return new ExampleResponse(example.band(), example.prompt(), example.essay(), example.commentary());
        }
    }
}
