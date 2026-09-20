package com.group01.user.application.usecase;

import com.group01.user.domain.repository.AccountActionTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RevokeUserActionTokensUseCase {
    private final AccountActionTokenRepository accountActionTokenRepository;

    @Transactional
    public void execute(UUID userId) {
        accountActionTokenRepository.deleteByUserId(userId);
    }
}

