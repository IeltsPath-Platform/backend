package com.ieltspath.library.application.port;
import java.time.Instant;
import java.util.*;

public interface StudyProgressReader {
    record PublishedUnit(UUID id,UUID topicId,int sortOrder,int revision,int curriculumVersion) {}
    record Progress(UUID unitId,Instant firstCompletedAt,Instant lastCompletedAt,int bestAccuracyPercent) {}
    record ActiveAttempt(UUID id,UUID unitId,String mode,int completedSteps,int totalSteps) {}
    record Snapshot(List<PublishedUnit> units,List<Progress> progress,List<ActiveAttempt> attempts,Map<UUID,Integer> versions) {}
    Snapshot read(UUID user,List<UUID> topics,Instant now);
}
