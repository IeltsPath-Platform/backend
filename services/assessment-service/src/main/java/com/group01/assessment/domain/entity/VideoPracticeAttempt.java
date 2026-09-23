package com.group01.assessment.domain.entity;
import com.group01.assessment.domain.vo.*; import java.time.Instant; import java.util.UUID;
public record VideoPracticeAttempt(UUID id,UUID userId,UUID videoId,PracticeType practiceType,PracticeStatus status,Instant startedAt,Instant completedAt,String resultSnapshot) {}
