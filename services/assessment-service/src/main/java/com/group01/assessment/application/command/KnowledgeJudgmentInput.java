package com.group01.assessment.application.command;

import com.group01.assessment.domain.vo.QualitativeJudgment;

import java.util.UUID;

/** A grader's explicit qualitative outcome for one knowledge point snapshotted on one attempt item. */
public record KnowledgeJudgmentInput(UUID attemptItemId, UUID knowledgePointId, QualitativeJudgment judgment) {
}
