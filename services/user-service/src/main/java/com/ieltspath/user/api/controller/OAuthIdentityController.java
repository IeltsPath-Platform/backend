package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.user.api.dto.response.MessageResponse;
import com.ieltspath.user.api.dto.response.OAuthIdentityResponse;
import com.ieltspath.user.application.result.OAuthIdentityResult;
import com.ieltspath.user.application.usecase.GetOAuthIdentitiesUseCase;
import com.ieltspath.user.application.usecase.UnlinkOAuthIdentityUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class OAuthIdentityController {

    private final GetOAuthIdentitiesUseCase getOAuthIdentitiesUseCase;
    private final UnlinkOAuthIdentityUseCase unlinkOAuthIdentityUseCase;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/me/oauth")
    public List<OAuthIdentityResponse> getMyOAuthIdentities() {
        UUID userId = currentUserProvider.requireUserId();
        return getOAuthIdentitiesUseCase.execute(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @DeleteMapping("/me/oauth/{provider}")
    public MessageResponse unlinkMyOAuthIdentity(@PathVariable("provider") String provider) {
        UUID userId = currentUserProvider.requireUserId();
        unlinkOAuthIdentityUseCase.execute(userId, provider);
        return new MessageResponse("Hủy liên kết tài khoản OAuth thành công");
    }

    private OAuthIdentityResponse toResponse(OAuthIdentityResult result) {
        return new OAuthIdentityResponse(
                result.id(),
                result.userId(),
                result.provider(),
                result.providerSubject(),
                result.linkedAt(),
                result.lastAuthenticatedAt()
        );
    }
}
