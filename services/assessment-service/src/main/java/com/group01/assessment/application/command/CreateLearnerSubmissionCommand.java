package com.group01.assessment.application.command;

import com.group01.assessment.domain.vo.Skill;

import java.util.UUID;

public record CreateLearnerSubmissionCommand(UUID userId, UUID attemptItemId, String promptSnapshot,
                                             Skill skill, String textPayload, String audioReference,
                                             String submissionKey) {}
