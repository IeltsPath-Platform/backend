package com.group01.assessment.domain.entity;

import com.group01.assessment.domain.vo.Skill;
import com.group01.assessment.domain.vo.SubmissionStatus;

import java.time.Instant;
import java.util.UUID;

public record LearnerSubmission(UUID id, UUID userId, UUID attemptItemId, String promptSnapshot,
                                Skill skill, String textPayload, String audioReference,
                                SubmissionStatus status, String submissionKey, Instant submittedAt) {}
