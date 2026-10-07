package com.ieltspath.assessment.application.command;

import com.ieltspath.assessment.domain.vo.AttemptChannel;
import com.ieltspath.assessment.domain.vo.AttemptMode;

import java.util.UUID;

/** Everything else (type, sections, items, answers, knowledge points) comes from the Content package version. */
public record StartAssessmentAttemptCommand(UUID userId, UUID packageVersionId, AttemptMode mode,
                                            AttemptChannel channel) {}
