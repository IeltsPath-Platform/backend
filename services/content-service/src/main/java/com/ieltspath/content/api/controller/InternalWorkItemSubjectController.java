package com.ieltspath.content.api.controller;

import com.ieltspath.content.application.result.WorkItemSubjectsResult;
import com.ieltspath.content.application.usecase.GetWorkItemSubjectsUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Lets Learning name the knowledge points and questions its staff work items are about, in one call. */
@RestController
@RequestMapping("/internal/learning-content/work-item-subjects")
public class InternalWorkItemSubjectController {
    private final GetWorkItemSubjectsUseCase subjects;

    public InternalWorkItemSubjectController(GetWorkItemSubjectsUseCase subjects) {
        this.subjects = subjects;
    }

    @PostMapping
    public WorkItemSubjectsResult subjects(@Valid @RequestBody Request request) {
        return subjects.execute(request.knowledgePointIds() == null ? List.of() : request.knowledgePointIds(),
                request.questionVersionIds() == null ? List.of() : request.questionVersionIds());
    }

    public record Request(@Size(max = 500) List<@NotNull UUID> knowledgePointIds,
                          @Size(max = 500) List<@NotNull UUID> questionVersionIds) {
    }
}
