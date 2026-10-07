package com.ieltspath.assessment.domain.entity;

import com.ieltspath.assessment.domain.vo.Skill;
import com.ieltspath.assessment.domain.vo.SubmissionStatus;

import java.time.Instant;
import java.util.UUID;

public record LearnerSubmission(UUID id, UUID userId, UUID attemptItemId, String promptSnapshot,
                                Skill skill, String textPayload, String audioReference,
                                SubmissionStatus status, String submissionKey, Instant submittedAt) {}
