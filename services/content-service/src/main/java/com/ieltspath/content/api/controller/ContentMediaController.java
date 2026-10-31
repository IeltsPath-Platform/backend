package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.commonstorage.UploadRejectedException;
import com.ieltspath.content.api.dto.response.StoredAudioResponse;
import com.ieltspath.content.application.result.StoredMediaResult;
import com.ieltspath.content.application.usecase.UploadContentAudioUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** Stores an audio file and returns its key; the author then attaches the key to a section. */
@RestController
@RequestMapping("/api/content/admin/media")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class ContentMediaController {

    private final UploadContentAudioUseCase uploadAudio;
    private final CurrentUserProvider currentUser;

    public ContentMediaController(UploadContentAudioUseCase uploadAudio, CurrentUserProvider currentUser) {
        this.uploadAudio = uploadAudio;
        this.currentUser = currentUser;
    }

    @PostMapping(value = "/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public StoredAudioResponse upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UploadRejectedException(UploadRejectedException.Reason.EMPTY);
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException unreadable) {
            throw new UploadRejectedException(UploadRejectedException.Reason.EMPTY);
        }
        StoredMediaResult stored = uploadAudio.execute(currentUser.requireUserId(), file.getContentType(), bytes);
        return StoredAudioResponse.from(stored);
    }
}
