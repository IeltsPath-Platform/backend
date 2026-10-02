package com.group01.learning.api.dto.response;

import com.group01.learning.application.result.MasteryResult;
import java.util.UUID;

public record MasteryResponse(UUID knowledgePointId, UUID topicId, double mastery, long evidenceCount) {
    public static MasteryResponse from(MasteryResult result) {
        return new MasteryResponse(result.knowledgePointId(), result.topicId(), result.mastery(), result.evidenceCount());
    }
}
