package com.group01.user.infrastructure.adapter;

import com.group01.user.domain.aggregate.OAuthIdentity;
import com.group01.user.domain.repository.OAuthIdentityRepository;
import com.group01.user.domain.vo.OAuthProvider;
import com.group01.user.infrastructure.persistence.entity.OAuthIdentityJpaEntity;
import com.group01.user.infrastructure.persistence.mapper.OAuthIdentityMapper;
import com.group01.user.infrastructure.persistence.repository.OAuthIdentityJpaRepository;
import com.group01.user.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OAuthIdentityRepositoryAdapter implements OAuthIdentityRepository {
    private final OAuthIdentityJpaRepository oauthIdentityJpaRepository;
    private final OAuthIdentityMapper oauthIdentityMapper;
    private final UserJpaRepository userJpaRepository;

    @Override
    public OAuthIdentity save(OAuthIdentity oauthIdentity) {
        OAuthIdentityJpaEntity entity = oauthIdentityMapper.toEntity(oauthIdentity);
        if (entity.getUser() == null && oauthIdentity.getUserId() != null) {
            entity.setUser(userJpaRepository.getReferenceById(oauthIdentity.getUserId()));
        }
        return oauthIdentityMapper.toDomain(oauthIdentityJpaRepository.save(entity));
    }

    @Override
    public Optional<OAuthIdentity> findById(UUID id) {
        return oauthIdentityJpaRepository.findById(id).map(oauthIdentityMapper::toDomain);
    }

    @Override
    public Optional<OAuthIdentity> findByProviderAndProviderSubject(OAuthProvider provider, String providerSubject) {
        return oauthIdentityJpaRepository.findByProviderAndProviderSubject(provider, providerSubject).map(oauthIdentityMapper::toDomain);
    }

    @Override
    public List<OAuthIdentity> findByUserId(UUID userId) {
        return oauthIdentityJpaRepository.findByUser_Id(userId).stream()
                .map(oauthIdentityMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<OAuthIdentity> findByUserIdAndProvider(UUID userId, OAuthProvider provider) {
        return oauthIdentityJpaRepository.findByUser_IdAndProvider(userId, provider).map(oauthIdentityMapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        oauthIdentityJpaRepository.deleteById(id);
    }

    @Override
    public void deleteByUserIdAndProvider(UUID userId, OAuthProvider provider) {
        oauthIdentityJpaRepository.deleteByUser_IdAndProvider(userId, provider);
    }
}

