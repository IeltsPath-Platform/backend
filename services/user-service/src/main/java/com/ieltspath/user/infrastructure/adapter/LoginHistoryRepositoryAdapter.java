package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.domain.aggregate.LoginAttempt;
import com.ieltspath.user.domain.repository.LoginHistoryRepository;
import com.ieltspath.user.domain.vo.LoginFailureReason;
import com.ieltspath.user.domain.vo.OwnedPage;
import com.ieltspath.user.infrastructure.persistence.entity.LoginHistoryJpaEntity;
import com.ieltspath.user.infrastructure.persistence.repository.LoginHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LoginHistoryRepositoryAdapter implements LoginHistoryRepository {
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"));

    private final LoginHistoryJpaRepository loginHistoryJpaRepository;

    @Override
    public LoginAttempt save(LoginAttempt attempt) {
        return toDomain(loginHistoryJpaRepository.save(toEntity(attempt)));
    }

    @Override
    public OwnedPage<LoginAttempt> findByUserId(UUID userId, int page, int size) {
        Page<LoginHistoryJpaEntity> result = loginHistoryJpaRepository.findByUserId(
                userId, PageRequest.of(page, size, NEWEST_FIRST));
        return new OwnedPage<>(result.getContent().stream().map(this::toDomain).toList(), result.getTotalElements());
    }

    @Override
    public int deleteOlderThan(LocalDateTime cutoff) {
        return loginHistoryJpaRepository.deleteOlderThan(cutoff);
    }

    private LoginAttempt toDomain(LoginHistoryJpaEntity entity) {
        return LoginAttempt.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .success(entity.isSuccess())
                .failureReason(entity.getFailureReason() == null ? null : LoginFailureReason.valueOf(entity.getFailureReason()))
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private LoginHistoryJpaEntity toEntity(LoginAttempt attempt) {
        return LoginHistoryJpaEntity.builder()
                .id(attempt.getId())
                .userId(attempt.getUserId())
                .success(attempt.isSuccess())
                .failureReason(attempt.getFailureReason() == null ? null : attempt.getFailureReason().name())
                .ipAddress(attempt.getIpAddress())
                .userAgent(attempt.getUserAgent())
                .createdAt(attempt.getCreatedAt())
                .build();
    }
}
