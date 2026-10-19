package com.ieltspath.community.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.commonsecurity.role.CanonicalRoles;
import com.ieltspath.commonstorage.UploadRejectedException;
import com.ieltspath.community.api.dto.response.StoredMediaResponse;
import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.result.StoredMediaResult;
import com.ieltspath.community.application.usecase.UploadCommunityImageUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/community/media")
@RequiredArgsConstructor
public class CommunityMediaController {

    private final CurrentUserProvider currentUserProvider;
    private final UploadCommunityImageUseCase uploadImageUseCase;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public StoredMediaResponse uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UploadRejectedException(UploadRejectedException.Reason.EMPTY);
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UploadRejectedException(UploadRejectedException.Reason.EMPTY);
        }
        StoredMediaResult result = uploadImageUseCase.execute(actor(), file.getContentType(), bytes);
        return StoredMediaResponse.from(result);
    }

    private CommunityActor actor() {
        var user = currentUserProvider.requireCurrentUser();
        return new CommunityActor(user.id(), user.hasRole(CanonicalRoles.ADMIN));
    }
}

