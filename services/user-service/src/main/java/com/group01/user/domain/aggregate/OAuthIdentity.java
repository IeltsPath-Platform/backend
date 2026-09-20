package com.group01.user.domain.aggregate;

import com.group01.user.domain.vo.OAuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class OAuthIdentity {
    private UUID id;
    private UUID userId;
    private OAuthProvider provider;
    private String providerSubject;
    private LocalDateTime linkedAt;
    private LocalDateTime lastAuthenticatedAt;

    public void recordAuthentication() {
        this.lastAuthenticatedAt = LocalDateTime.now();
    }
}

