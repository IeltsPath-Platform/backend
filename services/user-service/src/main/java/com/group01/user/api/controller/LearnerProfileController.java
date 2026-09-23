package com.group01.user.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.user.api.dto.request.UpdateLearnerProfileRequest;
import com.group01.user.api.dto.response.LearnerProfileResponse;
import com.group01.user.application.command.UpdateLearnerProfileCommand;
import com.group01.user.application.result.LearnerProfileResult;
import com.group01.user.application.usecase.GetLearnerProfileUseCase;
import com.group01.user.application.usecase.UpdateLearnerProfileUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class LearnerProfileController {

    private final GetLearnerProfileUseCase getLearnerProfileUseCase;
    private final UpdateLearnerProfileUseCase updateLearnerProfileUseCase;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/me/profile")
    public LearnerProfileResponse getMyProfile() {
        UUID userId = currentUserProvider.requireUserId();
        return toResponse(getLearnerProfileUseCase.execute(userId));
    }

    @PutMapping("/me/profile")
    public LearnerProfileResponse updateMyProfile(@Valid @RequestBody UpdateLearnerProfileRequest request) {
        UUID userId = currentUserProvider.requireUserId();
        return toResponse(updateLearnerProfileUseCase.execute(new UpdateLearnerProfileCommand(
                userId,
                request.displayName(),
                request.avatarReference(),
                request.bio(),
                request.selfReportedBand(),
                request.timezone(),
                request.visibility()
        )));
    }

    private LearnerProfileResponse toResponse(LearnerProfileResult result) {
        return new LearnerProfileResponse(
                result.userId(),
                result.displayName(),
                result.avatarReference(),
                result.bio(),
                result.selfReportedBand(),
                result.timezone(),
                result.visibility(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
