package com.group01.assessment.api.dto.response;
import com.group01.assessment.domain.entity.AttemptResponse; import java.time.Instant; import java.util.UUID;
public record AttemptResponseResponse(UUID id,UUID attemptItemId,String payload,int schemaVersion,long revision,Instant savedAt,Instant submittedAt){public static AttemptResponseResponse from(AttemptResponse r){return new AttemptResponseResponse(r.id(),r.attemptItemId(),r.payload(),r.schemaVersion(),r.revision(),r.savedAt(),r.submittedAt());}}
