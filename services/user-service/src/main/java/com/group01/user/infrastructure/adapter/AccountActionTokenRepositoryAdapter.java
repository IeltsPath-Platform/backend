package com.group01.user.infrastructure.adapter;

import com.group01.user.domain.aggregate.AccountActionToken;
import com.group01.user.domain.repository.AccountActionTokenRepository;
import com.group01.user.domain.vo.ActionTokenPurpose;
import com.group01.user.infrastructure.persistence.entity.AccountActionTokenJpaEntity;
import com.group01.user.infrastructure.persistence.mapper.AccountActionTokenMapper;
import com.group01.user.infrastructure.persistence.repository.AccountActionTokenJpaRepository;
import com.group01.user.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AccountActionTokenRepositoryAdapter implements AccountActionTokenRepository {
    private final AccountActionTokenJpaRepository accountActionTokenJpaRepository;
    private final AccountActionTokenMapper accountActionTokenMapper;
    private final UserJpaRepository userJpaRepository;

    @Override
    public AccountActionToken save(AccountActionToken token) {
        AccountActionTokenJpaEntity entity = accountActionTokenMapper.toEntity(token);
        if (entity.getUser() == null && token.getUserId() != null) {
            entity.setUser(userJpaRepository.getReferenceById(token.getUserId()));
        }
        return accountActionTokenMapper.toDomain(accountActionTokenJpaRepository.save(entity));
    }

    @Override
    public Optional<AccountActionToken> findById(UUID id) {
        return accountActionTokenJpaRepository.findById(id).map(accountActionTokenMapper::toDomain);
    }

    @Override
    public Optional<AccountActionToken> findByTokenHash(String tokenHash) {
        return accountActionTokenJpaRepository.findByTokenHash(tokenHash).map(accountActionTokenMapper::toDomain);
    }

    @Override
    public Optional<AccountActionToken> findByTokenHashAndPurpose(String tokenHash, ActionTokenPurpose purpose) {
        return accountActionTokenJpaRepository.findByTokenHashAndPurpose(tokenHash, purpose).map(accountActionTokenMapper::toDomain);
    }

    @Override
    public List<AccountActionToken> findByUserId(UUID userId) {
        return accountActionTokenJpaRepository.findByUser_Id(userId).stream()
                .map(accountActionTokenMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        accountActionTokenJpaRepository.deleteById(id);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        accountActionTokenJpaRepository.deleteByUser_Id(userId);
    }
}

