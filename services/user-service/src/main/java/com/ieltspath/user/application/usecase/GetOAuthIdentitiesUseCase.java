package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.result.OAuthIdentityResult;
import com.ieltspath.user.domain.aggregate.OAuthIdentity;
import com.ieltspath.user.domain.repository.OAuthIdentityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetOAuthIdentitiesUseCase {
    private final OAuthIdentityRepository oauthIdentityRepository;

    @Transactional(readOnly = true)
    public List<OAuthIdentityResult> execute(UUID userId) {
        return oauthIdentityRepository.findByUserId(userId).stream()
                .map(this::toResult)
                .toList();
    }

    private OAuthIdentityResult toResult(OAuthIdentity identity) {
        return new OAuthIdentityResult(
                identity.getId(),
                identity.getUserId(),
                identity.getProvider().name(),
                identity.getProviderSubject(),
                identity.getLinkedAt(),
                identity.getLastAuthenticatedAt()
        );
    }
}

