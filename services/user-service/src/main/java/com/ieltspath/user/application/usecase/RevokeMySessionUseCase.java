package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.exception.SessionNotFoundException;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RevokeMySessionUseCase {
    private final RefreshTokenRepository tokens;

    @Transactional
    public void execute(UUID userId, UUID sessionId) {
        if (tokens.revokeSession(userId, sessionId, LocalDateTime.now(), "SESSION_REVOKED") == 0) {
            throw new SessionNotFoundException();
        }
    }
}
