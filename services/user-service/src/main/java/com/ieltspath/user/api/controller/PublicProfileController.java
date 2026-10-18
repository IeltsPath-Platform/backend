package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.user.api.dto.request.PublicProfilesRequest;
import com.ieltspath.user.api.dto.response.PublicProfileResponse;
import com.ieltspath.user.api.dto.response.PublicProfilesResponse;
import com.ieltspath.user.application.usecase.GetPublicProfilesUseCase;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PublicProfileController {
    private final GetPublicProfilesUseCase useCase;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/api/users/{userId}/public-profile")
    public PublicProfileResponse get(@PathVariable("userId") UUID userId) {
        return PublicProfileResponse.from(useCase.execute(currentUserProvider.requireUserId(), userId));
    }

    @Hidden
    @PostMapping("/internal/users/public-profiles")
    public PublicProfilesResponse getBatch(@Valid @RequestBody PublicProfilesRequest request) {
        return new PublicProfilesResponse(useCase.executeBatch(currentUserProvider.requireUserId(), request.userIds())
                .stream().map(PublicProfileResponse::from).toList());
    }

    @Hidden
    @GetMapping("/internal/users/public-profiles/search")
    public PublicProfilesResponse search(@RequestParam("q") String q, @RequestParam(name = "limit", defaultValue = "8") int limit) {
        return new PublicProfilesResponse(useCase.search(currentUserProvider.requireUserId(), q, limit).stream()
                .map(PublicProfileResponse::from).toList());
    }
}
