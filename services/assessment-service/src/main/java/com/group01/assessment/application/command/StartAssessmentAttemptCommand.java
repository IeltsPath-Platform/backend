package com.group01.assessment.application.command;

import com.group01.assessment.domain.vo.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StartAssessmentAttemptCommand(UUID userId, UUID packageVersionId, AttemptType attemptType,
                                            AttemptMode mode, AttemptChannel channel, Instant expiresAt,
                                            List<SectionInput> sections) {
    public record SectionInput(UUID contentSectionId, int sortOrder, String snapshot, List<ItemInput> items) {}
    public record ItemInput(UUID questionVersionId, int sortOrder, String questionSnapshot,
                            String answerSnapshot, String knowledgeSnapshot) {}
}
