package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.usecase.GetSpeakingRecordingUrlUseCase;
import com.ieltspath.assessment.application.usecase.UploadSpeakingRecordingUseCase;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/assessments")
public class SpeakingRecordingController {
    private final CurrentUserProvider currentUser;
    private final UploadSpeakingRecordingUseCase upload;
    private final GetSpeakingRecordingUrlUseCase recordingUrl;

    public SpeakingRecordingController(CurrentUserProvider currentUser,
                                       UploadSpeakingRecordingUseCase upload,
                                       GetSpeakingRecordingUrlUseCase recordingUrl) {
        this.currentUser = currentUser;
        this.upload = upload;
        this.recordingUrl = recordingUrl;
    }

    @PostMapping(path = "/speaking-recordings", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public UploadSpeakingRecordingUseCase.RecordingResult upload(@RequestParam("file") @NotNull MultipartFile file)
            throws IOException {
        return upload.execute(currentUser.requireUserId(), file.getContentType(), file.getBytes());
    }

    @GetMapping("/submissions/{submissionId}/recording-url")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EXAMINER')")
    public RecordingUrlResponse recordingUrl(@PathVariable UUID submissionId) {
        var result = recordingUrl.execute(submissionId, currentUser.requireUserId());
        return new RecordingUrlResponse(result.url(), result.expiresInSeconds());
    }

    public record RecordingUrlResponse(String url, int expiresInSeconds) { }
}
