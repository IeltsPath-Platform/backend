package com.group01.assessment.api.dto.response;
import com.group01.assessment.application.result.LearnerSubmissionResult; import com.group01.assessment.domain.vo.*; import java.time.Instant; import java.util.UUID;
public record LearnerSubmissionResponse(UUID id,UUID userId,UUID attemptItemId,Skill skill,SubmissionStatus status,String submissionKey,Instant submittedAt){public static LearnerSubmissionResponse from(LearnerSubmissionResult r){return new LearnerSubmissionResponse(r.id(),r.userId(),r.attemptItemId(),r.skill(),r.status(),r.submissionKey(),r.submittedAt());}}
