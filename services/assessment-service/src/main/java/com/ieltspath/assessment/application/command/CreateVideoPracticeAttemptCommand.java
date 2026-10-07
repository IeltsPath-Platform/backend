package com.ieltspath.assessment.application.command;

import com.ieltspath.assessment.domain.vo.PracticeType;

import java.util.UUID;

public record CreateVideoPracticeAttemptCommand(UUID userId, UUID videoId, UUID segmentId,
                                                PracticeType practiceType,
                                                String referenceTextSnapshot) {
}
