package com.ieltspath.user.domain.repository;

import com.ieltspath.user.domain.aggregate.OAuthIdentity;
import com.ieltspath.user.domain.vo.OAuthProvider;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OAuthIdentityRepository {
    OAuthIdentity save(OAuthIdentity oauthIdentity);
    Optional<OAuthIdentity> findById(UUID id);
    Optional<OAuthIdentity> findByProviderAndProviderSubject(OAuthProvider provider, String providerSubject);
    List<OAuthIdentity> findByUserId(UUID userId);
    Optional<OAuthIdentity> findByUserIdAndProvider(UUID userId, OAuthProvider provider);
    void deleteById(UUID id);
    void deleteByUserIdAndProvider(UUID userId, OAuthProvider provider);
}

