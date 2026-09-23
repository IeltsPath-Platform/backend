package com.group01.assessment.infrastructure.persistence.mapper;

import com.group01.assessment.domain.aggregate.AssessmentAttempt;
import com.group01.assessment.domain.entity.*;
import com.group01.assessment.infrastructure.persistence.entity.*;
import org.springframework.stereotype.Component;

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
    public AssessmentResult toDomain(AssessmentResultJpaEntity e) { return new AssessmentResult(e.getId(), e.getAttemptId(), e.getResultVersion(), e.getStatus(), e.getOverallBand(), e.getCompletedAt()); }
    public AssessmentResultJpaEntity toEntity(AssessmentResult d) {
        var e = new AssessmentResultJpaEntity(); e.setId(d.id()); e.setAttemptId(d.attemptId()); e.setResultVersion(d.resultVersion()); e.setStatus(d.status()); e.setOverallBand(d.overallBand()); e.setCompletedAt(d.completedAt()); return e;
    }
    public LearnerSubmission toDomain(LearnerSubmissionJpaEntity e) { return new LearnerSubmission(e.getId(), e.getUserId(), e.getAttemptItemId(), e.getPromptSnapshot(), e.getSkill(), e.getTextPayload(), e.getAudioReference(), e.getStatus(), e.getSubmissionKey(), e.getSubmittedAt()); }
    public LearnerSubmissionJpaEntity toEntity(LearnerSubmission d) {
        var e = new LearnerSubmissionJpaEntity(); e.setId(d.id()); e.setUserId(d.userId()); e.setAttemptItemId(d.attemptItemId()); e.setPromptSnapshot(d.promptSnapshot()); e.setSkill(d.skill()); e.setTextPayload(d.textPayload()); e.setAudioReference(d.audioReference()); e.setStatus(d.status()); e.setSubmissionKey(d.submissionKey()); e.setSubmittedAt(d.submittedAt()); return e;
    }
    public GradingJob toDomain(GradingJobJpaEntity e) { return new GradingJob(e.getId(), e.getSubmissionId(), e.getUserId(), e.getSkill(), e.getGradingMode(), e.getStatus(), e.getPointCostSnapshot(), e.getIdempotencyKey(), e.getCreatedAt(), e.getCompletedAt()); }
    public GradingJobJpaEntity toEntity(GradingJob d) {
        var e = new GradingJobJpaEntity(); e.setId(d.id()); e.setSubmissionId(d.submissionId()); e.setUserId(d.userId()); e.setSkill(d.skill()); e.setGradingMode(d.gradingMode()); e.setStatus(d.status()); e.setPointCostSnapshot(d.pointCostSnapshot()); e.setIdempotencyKey(d.idempotencyKey()); e.setCreatedAt(d.createdAt()); e.setCompletedAt(d.completedAt()); return e;
    }
    public SkillScore toDomain(SkillScoreJpaEntity e){return new SkillScore(e.getId(),e.getResultId(),e.getSkill(),e.getRawScore(),e.getBand(),e.getGradingSource(),e.getFeedbackRevisionId());}
    public SkillScoreJpaEntity toEntity(SkillScore d){var e=new SkillScoreJpaEntity();e.setId(d.id());e.setResultId(d.resultId());e.setSkill(d.skill());e.setRawScore(d.rawScore());e.setBand(d.band());e.setGradingSource(d.gradingSource());e.setFeedbackRevisionId(d.feedbackRevisionId());return e;}
    public ItemResult toDomain(ItemResultJpaEntity e){return new ItemResult(e.getId(),e.getResultId(),e.getAttemptItemId(),e.getRawScore(),e.getMaxScore(),e.getCorrect(),e.getGradingSource());}
    public ItemResultJpaEntity toEntity(ItemResult d){var e=new ItemResultJpaEntity();e.setId(d.id());e.setResultId(d.resultId());e.setAttemptItemId(d.attemptItemId());e.setRawScore(d.rawScore());e.setMaxScore(d.maxScore());e.setCorrect(d.correct());e.setGradingSource(d.gradingSource());return e;}
    public ErrorAnalysisItem toDomain(ErrorAnalysisItemJpaEntity e){return new ErrorAnalysisItem(e.getId(),e.getResultId(),e.getAttemptItemId(),e.getTaxonomyCode(),e.getSeverity(),e.getNote());}
    public ErrorAnalysisItemJpaEntity toEntity(ErrorAnalysisItem d){var e=new ErrorAnalysisItemJpaEntity();e.setId(d.id());e.setResultId(d.resultId());e.setAttemptItemId(d.attemptItemId());e.setTaxonomyCode(d.taxonomyCode());e.setSeverity(d.severity());e.setNote(d.note());return e;}
    public VideoPracticeAttempt toDomain(VideoPracticeAttemptJpaEntity e){return new VideoPracticeAttempt(e.getId(),e.getUserId(),e.getVideoId(),e.getPracticeType(),e.getStatus(),e.getStartedAt(),e.getCompletedAt(),e.getResultSnapshot());}
    public VideoPracticeAttemptJpaEntity toEntity(VideoPracticeAttempt d){var e=new VideoPracticeAttemptJpaEntity();e.setId(d.id());e.setUserId(d.userId());e.setVideoId(d.videoId());e.setPracticeType(d.practiceType());e.setStatus(d.status());e.setStartedAt(d.startedAt());e.setCompletedAt(d.completedAt());e.setResultSnapshot(d.resultSnapshot());return e;}
}
