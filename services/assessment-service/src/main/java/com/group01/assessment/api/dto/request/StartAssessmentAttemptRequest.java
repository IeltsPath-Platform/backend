package com.group01.assessment.api.dto.request;
import com.group01.assessment.domain.vo.*; import jakarta.validation.Valid; import jakarta.validation.constraints.NotEmpty; import jakarta.validation.constraints.NotNull; import java.time.Instant; import java.util.List; import java.util.UUID;
public record StartAssessmentAttemptRequest(@NotNull UUID packageVersionId,@NotNull AttemptType attemptType,@NotNull AttemptMode mode,@NotNull AttemptChannel channel,Instant expiresAt,@NotEmpty List<@Valid SectionRequest> sections){
 public record SectionRequest(@NotNull UUID contentSectionId,int sortOrder,@NotNull String snapshot,@NotEmpty List<@Valid ItemRequest> items){}
 public record ItemRequest(@NotNull UUID questionVersionId,int sortOrder,@NotNull String questionSnapshot,String answerSnapshot,String knowledgeSnapshot){}
}
