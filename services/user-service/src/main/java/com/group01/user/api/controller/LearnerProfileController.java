package com.group01.user.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.user.api.dto.request.UpdateLearnerProfileRequest;
import com.group01.user.api.dto.response.LearnerProfileResponse;
import com.group01.user.api.dto.response.MessageResponse;
import com.group01.user.application.command.UpdateLearnerProfileCommand;
import com.group01.user.application.result.LearnerProfileResult;
import com.group01.user.application.usecase.DeleteLearnerProfileUseCase;
import com.group01.user.application.usecase.GetLearnerProfileUseCase;
import com.group01.user.application.usecase.UpdateLearnerProfileUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final DeleteLearnerProfileUseCase deleteLearnerProfileUseCase;
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

    @GetMapping("/{id}/profile")
    @PreAuthorize("hasRole('ADMIN') or #p0.toString() == authentication.name")
    public LearnerProfileResponse getProfileById(@PathVariable("id") UUID id) {
        return toResponse(getLearnerProfileUseCase.execute(id));
    }

    @PutMapping("/{id}/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public LearnerProfileResponse updateProfileById(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateLearnerProfileRequest request
    ) {
        return toResponse(updateLearnerProfileUseCase.execute(new UpdateLearnerProfileCommand(
                id,
                request.displayName(),
                request.avatarReference(),
                request.bio(),
                request.selfReportedBand(),
                request.timezone(),
                request.visibility()
        )));
    }

    @DeleteMapping("/{id}/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public MessageResponse deleteProfileById(@PathVariable("id") UUID id) {
        deleteLearnerProfileUseCase.execute(id);
        return new MessageResponse("Xóa hồ sơ học tập thành công");
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
