package com.group01.user.domain.repository;

import com.group01.user.domain.aggregate.AccountActionToken;
import com.group01.user.domain.vo.ActionTokenPurpose;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountActionTokenRepository {
    AccountActionToken save(AccountActionToken token);
    Optional<AccountActionToken> findById(UUID id);
    Optional<AccountActionToken> findByTokenHash(String tokenHash);
    Optional<AccountActionToken> findByTokenHashAndPurpose(String tokenHash, ActionTokenPurpose purpose);
    List<AccountActionToken> findByUserId(UUID userId);
    void deleteById(UUID id);
    void deleteByUserId(UUID userId);
}

