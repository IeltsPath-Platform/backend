package com.ieltspath.user.infrastructure.persistence.repository;

import com.ieltspath.user.domain.vo.OAuthProvider;
import com.ieltspath.user.infrastructure.persistence.entity.OAuthIdentityJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OAuthIdentityJpaRepository extends JpaRepository<OAuthIdentityJpaEntity, UUID> {
    Optional<OAuthIdentityJpaEntity> findByProviderAndProviderSubject(OAuthProvider provider, String providerSubject);
    List<OAuthIdentityJpaEntity> findByUser_Id(UUID userId);
    Optional<OAuthIdentityJpaEntity> findByUser_IdAndProvider(UUID userId, OAuthProvider provider);
    void deleteByUser_IdAndProvider(UUID userId, OAuthProvider provider);
}

