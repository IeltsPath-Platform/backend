package com.group01.content.api.controller;

import com.group01.content.api.dto.internal.KnowledgePointMappingRequest;
import com.group01.content.api.dto.internal.KnowledgePointMappingResponse;
import com.group01.content.application.usecase.GetQuestionKnowledgePointMappingsUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Service-facing reads used by Assessment Service to snapshot canonical content at attempt start. */
@RestController
@RequestMapping("/internal/assessment-content")
public class InternalAssessmentContentController {
    private final GetQuestionKnowledgePointMappingsUseCase getMappings;

    public InternalAssessmentContentController(GetQuestionKnowledgePointMappingsUseCase getMappings) {
        this.getMappings = getMappings;
    }

    @PostMapping("/knowledge-point-mappings")
    public KnowledgePointMappingResponse knowledgePointMappings(
            @Valid @RequestBody KnowledgePointMappingRequest request) {
        return KnowledgePointMappingResponse.from(getMappings.execute(request.questionVersionIds()));
    }
}
