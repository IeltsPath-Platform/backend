package com.group01.user.application.usecase;

import com.group01.user.application.result.AccountActionTokenResult;
import com.group01.user.domain.aggregate.AccountActionToken;
import com.group01.user.domain.repository.AccountActionTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetActionTokensByUserIdUseCase {
    private final AccountActionTokenRepository accountActionTokenRepository;

    @Transactional(readOnly = true)
    public List<AccountActionTokenResult> execute(UUID userId) {
        return accountActionTokenRepository.findByUserId(userId).stream()
                .map(this::toResult)
                .toList();
    }

    private AccountActionTokenResult toResult(AccountActionToken token) {
        return new AccountActionTokenResult(
                token.getId(),
                token.getUserId(),
                token.getPurpose().name(),
                token.getExpiresAt(),
                token.getUsedAt(),
                token.getCreatedAt()
        );
    }
}

