package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.port.SpeakingTaskReader;

import java.util.List;
import java.util.UUID;

public record SpeakingTaskResponse(UUID packageId, UUID packageVersionId, UUID sectionId, String packageTitle,
                                   String title, Integer partNo, Integer timeLimitSeconds, String instructions,
                                   List<Question> questions) {
    public record Question(UUID questionVersionId, int sortOrder, String prompt, Integer durationSeconds) {}

    public static SpeakingTaskResponse from(SpeakingTaskReader.SpeakingTask task) {
        return new SpeakingTaskResponse(task.packageId(), task.packageVersionId(), task.sectionId(),
                task.packageTitle(), task.title(), task.partNo(), task.timeLimitSeconds(), task.instructions(),
                task.questions().stream().map(question -> new Question(question.questionVersionId(),
                        question.sortOrder(), question.stem(), question.durationSeconds())).toList());
    }
}
