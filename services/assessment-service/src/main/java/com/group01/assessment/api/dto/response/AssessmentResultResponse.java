package com.group01.assessment.api.dto.response;
import com.group01.assessment.application.result.AssessmentResultResult; import java.time.Instant; import java.util.UUID;
public record AssessmentResultResponse(UUID id,UUID attemptId,int resultVersion,String status,Double overallBand,Instant completedAt){public static AssessmentResultResponse from(AssessmentResultResult r){return new AssessmentResultResponse(r.id(),r.attemptId(),r.resultVersion(),r.status(),r.overallBand(),r.completedAt());}}
