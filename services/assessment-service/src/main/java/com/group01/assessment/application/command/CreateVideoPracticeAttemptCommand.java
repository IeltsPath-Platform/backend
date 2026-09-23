package com.group01.assessment.application.command; import com.group01.assessment.domain.vo.PracticeType; import java.util.UUID;
public record CreateVideoPracticeAttemptCommand(UUID userId,UUID videoId,PracticeType practiceType) {}
