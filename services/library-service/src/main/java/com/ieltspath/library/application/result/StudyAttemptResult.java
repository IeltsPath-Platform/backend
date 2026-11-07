package com.ieltspath.library.application.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.*;

public final class StudyAttemptResult {
    private StudyAttemptResult() {}
    public record Attempt(UUID attemptId,UUID topicId,UUID unitId,int revision,String status,String mode,Instant startedAt,
        Instant expiresAt,UUID currentStepId,List<UUID> completedStepIds,List<Map<String,Object>> steps,Completion completion) {}
    public record Start(Attempt attempt,boolean created) {}
    public record StepResult(UUID attemptId,UUID stepId,UUID submissionId,String outcome,Map<String,Object> correctAnswer,
        String explanationVi,boolean stepCompleted,int completedStepCount,int totalStepCount,UUID nextStepId,boolean canComplete,
        boolean heartLost,Map<String,Object> hearts) {}
    public record UnitProgress(UUID unitId,String status,UUID prerequisiteUnitId,UUID currentAttemptId,int completedStepCount,
        int totalStepCount,Instant firstCompletedAt,Instant lastCompletedAt,Integer bestAccuracyPercent) {}
    public record TopicProgress(UUID topicId,int curriculumVersion,int totalUnits,int completedUnits,UUID currentUnitId,
        List<UnitProgress> units,Instant updatedAt) {}
    public record Completion(UUID attemptId,UUID unitId,UUID topicId,Instant completedAt,int studiedWordCount,int gradedStepCount,int firstTryCorrectCount,
        int accuracyPercent,long durationSeconds,boolean firstCompletion,TopicProgress topicProgress,List<UUID> unlockedUnitIds,
        UUID nextUnitId,@JsonInclude(JsonInclude.Include.NON_NULL) Map<String,Object> reward) {}
}
