package com.group01.assessment.domain.repository;
import com.group01.assessment.domain.entity.VideoPracticeAttempt; import java.util.Optional; import java.util.UUID;
public interface VideoPracticeAttemptRepository { VideoPracticeAttempt save(VideoPracticeAttempt value); Optional<VideoPracticeAttempt> findByIdAndUserId(UUID id,UUID userId); }
