package com.group01.assessment.infrastructure.persistence.mapper;

import com.group01.assessment.domain.aggregate.AssessmentAttempt;
import com.group01.assessment.domain.entity.*;
import com.group01.assessment.infrastructure.persistence.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AssessmentPersistenceMapper {
    public AssessmentAttempt toDomain(AssessmentAttemptJpaEntity e) {
        return new AssessmentAttempt(e.getId(), e.getUserId(), e.getPackageVersionId(), e.getAttemptType(), e.getMode(),
                e.getChannel(), e.getStatus(), e.getStartedAt(), e.getSubmittedAt(), e.getExpiresAt(),
                e.getRowVersion(), e.getCreatedAt(), e.getUpdatedAt());
    }
    public AssessmentAttemptJpaEntity toEntity(AssessmentAttempt d) {
        var e = new AssessmentAttemptJpaEntity();
        e.setId(d.getId()); e.setUserId(d.getUserId()); e.setPackageVersionId(d.getPackageVersionId());
        e.setAttemptType(d.getAttemptType()); e.setMode(d.getMode()); e.setChannel(d.getChannel()); e.setStatus(d.getStatus());
        e.setStartedAt(d.getStartedAt()); e.setSubmittedAt(d.getSubmittedAt()); e.setExpiresAt(d.getExpiresAt());
        e.setRowVersion(d.getRowVersion()); e.setCreatedAt(d.getCreatedAt()); e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }
    public AttemptSection toDomain(AttemptSectionJpaEntity e) { return new AttemptSection(e.getId(), e.getAttemptId(), e.getContentSectionId(), e.getSortOrder(), e.getSnapshot()); }
    public AttemptSectionJpaEntity toEntity(AttemptSection d) {
        var e = new AttemptSectionJpaEntity(); e.setId(d.id()); e.setAttemptId(d.attemptId()); e.setContentSectionId(d.contentSectionId()); e.setSortOrder(d.sortOrder()); e.setSnapshot(d.snapshot()); return e;
    }
    public AttemptItem toDomain(AttemptItemJpaEntity e) { return new AttemptItem(e.getId(), e.getAttemptSectionId(), e.getQuestionVersionId(), e.getSortOrder(), e.getQuestionSnapshot(), e.getAnswerSnapshot(), e.getKnowledgeSnapshot()); }
    public AttemptItemJpaEntity toEntity(AttemptItem d) {
        var e = new AttemptItemJpaEntity(); e.setId(d.id()); e.setAttemptSectionId(d.attemptSectionId()); e.setQuestionVersionId(d.questionVersionId()); e.setSortOrder(d.sortOrder()); e.setQuestionSnapshot(d.questionSnapshot()); e.setAnswerSnapshot(d.answerSnapshot()); e.setKnowledgeSnapshot(d.knowledgeSnapshot()); return e;
    }
    public AttemptResponse toDomain(AttemptResponseJpaEntity e) { return new AttemptResponse(e.getId(), e.getAttemptItemId(), e.getPayload(), e.getSchemaVersion(), e.getRevision(), e.getSavedAt(), e.getSubmittedAt(), e.getLockVersion()); }
    public AttemptResponseJpaEntity toEntity(AttemptResponse d) {
        var e = new AttemptResponseJpaEntity(); e.setId(d.id()); e.setAttemptItemId(d.attemptItemId()); e.setPayload(d.payload()); e.setSchemaVersion(d.schemaVersion()); e.setRevision(d.revision()); e.setLockVersion(d.lockVersion()); e.setSavedAt(d.savedAt()); e.setSubmittedAt(d.submittedAt()); return e;
    }

    public AssessmentResult toDomain(AssessmentResultJpaEntity e) {
        return new AssessmentResult(e.getId(), e.getAttemptId(), e.getResultVersion(), e.getStatus(), toDouble(e.getOverallBand()), e.getCompletedAt());
    }
    public AssessmentResultJpaEntity toEntity(AssessmentResult d) {
        var e = new AssessmentResultJpaEntity();
        e.setId(d.id());
        e.setAttemptId(d.attemptId());
        e.setResultVersion(d.resultVersion());
        e.setStatus(d.status());
        e.setOverallBand(toBigDecimal(d.overallBand()));
        e.setCompletedAt(d.completedAt());
        return e;
    }
    public LearnerSubmission toDomain(LearnerSubmissionJpaEntity e) { return new LearnerSubmission(e.getId(), e.getUserId(), e.getAttemptItemId(), e.getPromptSnapshot(), e.getSkill(), e.getTextPayload(), e.getAudioReference(), e.getStatus(), e.getSubmissionKey(), e.getSubmittedAt()); }
    public LearnerSubmissionJpaEntity toEntity(LearnerSubmission d) {
        var e = new LearnerSubmissionJpaEntity(); e.setId(d.id()); e.setUserId(d.userId()); e.setAttemptItemId(d.attemptItemId()); e.setPromptSnapshot(d.promptSnapshot()); e.setSkill(d.skill()); e.setTextPayload(d.textPayload()); e.setAudioReference(d.audioReference()); e.setStatus(d.status()); e.setSubmissionKey(d.submissionKey()); e.setSubmittedAt(d.submittedAt()); return e;
    }
    public GradingJob toDomain(GradingJobJpaEntity e) { return new GradingJob(e.getId(), e.getSubmissionId(), e.getUserId(), e.getSkill(), e.getGradingMode(), e.getStatus(), e.getPointCostSnapshot(), e.getIdempotencyKey(), e.getCreatedAt(), e.getCompletedAt()); }
    public GradingJobJpaEntity toEntity(GradingJob d) {
        var e = new GradingJobJpaEntity(); e.setId(d.id()); e.setSubmissionId(d.submissionId()); e.setUserId(d.userId()); e.setSkill(d.skill()); e.setGradingMode(d.gradingMode()); e.setStatus(d.status()); e.setPointCostSnapshot(d.pointCostSnapshot()); e.setIdempotencyKey(d.idempotencyKey()); e.setCreatedAt(d.createdAt()); e.setCompletedAt(d.completedAt()); return e;
    }

    public SkillScore toDomain(SkillScoreJpaEntity e) {
        return new SkillScore(e.getId(), e.getResultId(), e.getSkill(), toDouble(e.getRawScore()), toDouble(e.getBand()), e.getGradingSource(), e.getFeedbackRevisionId());
    }

    public SkillScoreJpaEntity toEntity(SkillScore d) {
        var e = new SkillScoreJpaEntity();
        e.setId(d.id());
        e.setResultId(d.resultId());
        e.setSkill(d.skill());
        e.setRawScore(toBigDecimal(d.rawScore()));
        e.setBand(toBigDecimal(d.band()));
        e.setGradingSource(d.gradingSource());
        e.setFeedbackRevisionId(d.feedbackRevisionId());
        return e;
    }

    public ItemResult toDomain(ItemResultJpaEntity e) {
        return new ItemResult(e.getId(), e.getResultId(), e.getAttemptItemId(), toDouble(e.getScore()), e.getCorrect(), e.getDurationMilliseconds(), e.getFeedbackSnapshot());
    }

    public ItemResultJpaEntity toEntity(ItemResult d) {
        var e = new ItemResultJpaEntity();
        e.setId(d.id());
        e.setResultId(d.resultId());
        e.setAttemptItemId(d.attemptItemId());
        e.setScore(toBigDecimal(d.score()));
        e.setCorrect(d.correct());
        e.setDurationMilliseconds(d.durationMilliseconds());
        e.setFeedbackSnapshot(d.feedbackSnapshot());
        return e;
    }

    public ErrorAnalysisItem toDomain(ErrorAnalysisItemJpaEntity e) {
        return new ErrorAnalysisItem(e.getId(), e.getItemResultId(), e.getKnowledgePointId(), e.getErrorType(), e.getExplanation());
    }

    public ErrorAnalysisItemJpaEntity toEntity(ErrorAnalysisItem d) {
        var e = new ErrorAnalysisItemJpaEntity();
        e.setId(d.id());
        e.setItemResultId(d.itemResultId());
        e.setKnowledgePointId(d.knowledgePointId());
        e.setErrorType(d.errorType());
        e.setExplanation(d.explanation());
        return e;
    }

    public VideoPracticeAttempt toDomain(VideoPracticeAttemptJpaEntity e) {
        return new VideoPracticeAttempt(e.getId(), e.getUserId(), e.getVideoId(), e.getSegmentId(), e.getPracticeType(), e.getReferenceTextSnapshot(), e.getResponseText(), e.getAudioReference(), e.getScore(), e.getStatus(), e.getStartedAt(), e.getCompletedAt(), e.getCreatedAt(), e.getResultPayload());
    }

    public VideoPracticeAttemptJpaEntity toEntity(VideoPracticeAttempt d) {
        var e = new VideoPracticeAttemptJpaEntity();
        e.setId(d.id());
        e.setUserId(d.userId());
        e.setVideoId(d.videoId());
        e.setSegmentId(d.segmentId());
        e.setPracticeType(d.practiceType());
        e.setReferenceTextSnapshot(d.referenceTextSnapshot());
        e.setResponseText(d.responseText());
        e.setAudioReference(d.audioReference());
        e.setScore(d.score());
        e.setStatus(d.status());
        e.setStartedAt(d.startedAt());
        e.setCompletedAt(d.completedAt());
        e.setCreatedAt(d.createdAt());
        e.setResultPayload(d.resultPayload());
        return e;
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
