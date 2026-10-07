package com.ieltspath.assessment.api.dto.request;
import com.ieltspath.assessment.domain.vo.Skill; import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull; import java.util.UUID;
public record CreateLearnerSubmissionRequest(UUID attemptItemId,@NotBlank String promptSnapshot,@NotNull Skill skill,String textPayload,String audioReference,@NotBlank String submissionKey) {}
