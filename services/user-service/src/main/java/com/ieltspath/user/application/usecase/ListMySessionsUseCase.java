package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.aggregate.RefreshToken;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListMySessionsUseCase {
    private final RefreshTokenRepository tokens;

    @Transactional(readOnly = true)
    public List<RefreshToken> execute(UUID userId) {
        return tokens.findActiveSessions(userId, LocalDateTime.now(), 50);
    }
}
