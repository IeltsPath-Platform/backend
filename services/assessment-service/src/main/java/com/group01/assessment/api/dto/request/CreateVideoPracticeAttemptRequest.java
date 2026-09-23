package com.group01.assessment.api.dto.request; import com.group01.assessment.domain.vo.PracticeType; import jakarta.validation.constraints.NotNull; import java.util.UUID;
public record CreateVideoPracticeAttemptRequest(@NotNull UUID videoId,@NotNull PracticeType practiceType) {}
