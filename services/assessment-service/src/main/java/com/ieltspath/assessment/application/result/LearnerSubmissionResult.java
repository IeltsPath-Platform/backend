package com.ieltspath.assessment.application.result;

import com.ieltspath.assessment.domain.vo.Skill;
import com.ieltspath.assessment.domain.vo.SubmissionStatus;

import java.time.Instant;
import java.util.UUID;

public record LearnerSubmissionResult(UUID id, UUID userId, UUID attemptItemId, Skill skill,
                                      SubmissionStatus status, String submissionKey, Instant submittedAt) {}
