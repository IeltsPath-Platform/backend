package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.user.api.dto.request.UpdateLearnerProfileRequest;
import com.ieltspath.user.api.dto.response.LearnerProfileResponse;
import com.ieltspath.user.application.command.UpdateLearnerProfileCommand;
import com.ieltspath.user.application.result.LearnerProfileResult;
import com.ieltspath.user.application.usecase.GetLearnerProfileUseCase;
import com.ieltspath.user.application.usecase.UpdateLearnerProfileUseCase;
import com.ieltspath.user.application.usecase.UploadAvatarUseCase;
import com.ieltspath.user.application.usecase.DeleteAvatarUseCase;
import com.ieltspath.commonstorage.UploadRejectedException;
import com.ieltspath.user.api.dto.request.ChangePasswordRequest;
import com.ieltspath.user.application.command.ChangePasswordCommand;
import com.ieltspath.user.application.usecase.ChangePasswordUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class LearnerProfileController {

    private final GetLearnerProfileUseCase getLearnerProfileUseCase;
    private final UpdateLearnerProfileUseCase updateLearnerProfileUseCase;
    private final CurrentUserProvider currentUserProvider;
    private final UploadAvatarUseCase uploadAvatarUseCase;
    private final DeleteAvatarUseCase deleteAvatarUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;

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
                request.bio(),
                request.selfReportedBand(),
                request.timezone(),
                request.visibility()
        )));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LearnerProfileResponse uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) throw new UploadRejectedException(UploadRejectedException.Reason.EMPTY);
        try {
            return toResponse(uploadAvatarUseCase.execute(currentUserProvider.requireUserId(), file.getBytes(), file.getContentType()));
        } catch (IOException e) {
            throw new UploadRejectedException(UploadRejectedException.Reason.EMPTY);
        }
    }

    @DeleteMapping("/me/avatar")
    public LearnerProfileResponse deleteAvatar() {
        return toResponse(deleteAvatarUseCase.execute(currentUserProvider.requireUserId()));
    }

    @PostMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        changePasswordUseCase.execute(new ChangePasswordCommand(
                currentUserProvider.requireUserId(), request.currentPassword(), request.newPassword()));
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
