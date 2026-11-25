package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.user.api.dto.request.ReauthRequest;
import com.ieltspath.user.api.dto.response.ReauthResponse;
import com.ieltspath.user.application.usecase.ReauthUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/reauth")
@RequiredArgsConstructor
public class ReauthController {
    private final ReauthUseCase reauth;
    private final CurrentUserProvider currentUser;

    @PostMapping
    public ReauthResponse reauthenticate(@Valid @RequestBody ReauthRequest request) {
        var result = reauth.execute(currentUser.requireUserId(), currentUser.currentSessionId().orElse(null),
                request.currentPassword(), request.googleIdToken());
        return new ReauthResponse(result.token(), 300, result.method());
    }
}
