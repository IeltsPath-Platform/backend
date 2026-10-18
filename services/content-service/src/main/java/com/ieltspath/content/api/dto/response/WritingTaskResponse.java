package com.ieltspath.content.api.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltspath.content.application.usecase.GetWritingTaskUseCase;

import java.util.List;
import java.util.UUID;

public record WritingTaskResponse(UUID packageId, UUID packageVersionId, UUID sectionId, UUID questionVersionId,
                                  String packageTitle, String title, Integer partNo, String task, Integer minWords,
                                  Integer timeLimitSeconds, String instructions, String prompt, String guide,
                                  JsonNode guideSteps, List<Image> images) {
    public record Image(String url, String altText) {}

    public static WritingTaskResponse from(GetWritingTaskUseCase.Result result) {
        var task = result.task();
        return new WritingTaskResponse(task.packageId(), task.packageVersionId(), task.sectionId(),
                task.questionVersionId(), task.packageTitle(), task.title(), task.partNo(), task.task(),
                task.minWords(), task.timeLimitSeconds(), task.instructions(), task.stem(), task.guide(),
                result.guideSteps(), result.images().stream().map(image -> new Image(image.url(), image.altText()))
                .toList());
    }
}
