package com.group01.content.api.controller;

import com.group01.content.api.dto.request.CreateKnowledgePointRequest;
import com.group01.content.api.dto.response.KnowledgePointResponse;
import com.group01.content.application.command.CreateKnowledgePointCommand;
import com.group01.content.application.result.KnowledgePointResult;
import com.group01.content.application.usecase.CreateKnowledgePointUseCase;
import com.group01.content.application.usecase.GetKnowledgePointsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/content/knowledge-points")
@RequiredArgsConstructor
public class KnowledgePointController {

    private final GetKnowledgePointsUseCase getKnowledgePointsUseCase;
    private final CreateKnowledgePointUseCase createKnowledgePointUseCase;

    @GetMapping
    public List<KnowledgePointResponse> getKnowledgePoints(
            @RequestParam(value = "topicId", required = false) UUID topicId
    ) {
        return getKnowledgePointsUseCase.execute(topicId).stream()
                .map(KnowledgePointResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KnowledgePointResponse createKnowledgePoint(@Valid @RequestBody CreateKnowledgePointRequest request) {
        KnowledgePointResult result = createKnowledgePointUseCase.execute(new CreateKnowledgePointCommand(
                request.topicId(),
                request.code(),
                request.name(),
                request.kind(),
                request.learningType(),
                request.skill(),
                request.description()
        ));
        return KnowledgePointResponse.from(result);
    }
}
