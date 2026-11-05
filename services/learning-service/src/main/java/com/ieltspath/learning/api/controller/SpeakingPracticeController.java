package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.request.CreateSpeakingSubmissionRequest;
import com.ieltspath.learning.application.result.SpeakingSubmissionResult;
import com.ieltspath.learning.application.service.SpeakingPracticeSettings;
import com.ieltspath.learning.application.usecase.CompleteSpeakingSubmissionUseCase;
import com.ieltspath.learning.application.usecase.CreateSpeakingSubmissionUseCase;
import com.ieltspath.learning.application.usecase.GetSpeakingSubmissionUseCase;
import com.ieltspath.learning.application.usecase.SettleSpeakingSubmissionUseCase;
import com.ieltspath.learning.application.usecase.UploadSpeakingRecordingUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Speaking practice graded by AI. The learner opens a submission (points are checked, nothing is charged), stores one
 * recording per question, then completes it so the grading can start.
 */
@RestController
@RequestMapping("/api/learning/speaking-practice")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class SpeakingPracticeController {
    private final CurrentUserProvider currentUser;
    private final CreateSpeakingSubmissionUseCase create;
    private final UploadSpeakingRecordingUseCase upload;
    private final CompleteSpeakingSubmissionUseCase complete;
    private final GetSpeakingSubmissionUseCase get;
    private final SettleSpeakingSubmissionUseCase settle;
    private final SpeakingPracticeSettings settings;

    /** What grading costs and the largest recording accepted, so the confirm dialog can show them before sending. */
    @GetMapping("/terms")
    public Terms terms() {
        return new Terms(settings.enabled(), settings.pointCost(), settings.maxRecordingBytes(), settings.retentionDays());
    }

    /** 402 {@code INSUFFICIENT_POINTS} when the balance is under the cost; no file is stored before this succeeds. */
    @PostMapping("/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public SpeakingSubmissionResult create(@Valid @RequestBody CreateSpeakingSubmissionRequest request) {
        return create.execute(currentUser.requireUserId(), request.requestId(), request.packageVersionId(),
                request.partSectionIds(), request.consent());
    }

    @PutMapping(path = "/submissions/{submissionId}/recordings/{questionVersionId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SpeakingSubmissionResult upload(@PathVariable UUID submissionId, @PathVariable UUID questionVersionId,
                                           @RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "durationMs", required = false) Integer durationMs)
            throws IOException {
        return upload.execute(currentUser.requireUserId(), submissionId, questionVersionId, file.getContentType(),
                file.getBytes(), durationMs);
    }

    @PostMapping("/submissions/{submissionId}/complete")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SpeakingSubmissionResult complete(@PathVariable UUID submissionId) {
        return complete.execute(currentUser.requireUserId(), submissionId);
    }

    /**
     * Charges a graded submission and releases its grade. Only the learner can spend their points, so this runs in
     * their request and not in the grading job. 402 {@code INSUFFICIENT_POINTS} leaves it waiting for a top-up; calling it
     * again is safe because the debit is idempotent.
     */
    @PostMapping("/submissions/{submissionId}/settle")
    public SpeakingSubmissionResult settle(@PathVariable UUID submissionId) {
        return settle.execute(currentUser.requireUserId(), submissionId);
    }

    @GetMapping("/submissions/{submissionId}")
    public SpeakingSubmissionResult get(@PathVariable UUID submissionId) {
        return get.execute(currentUser.requireUserId(), submissionId);
    }

    public record Terms(boolean enabled, int pointCost, long maxRecordingBytes, int retentionDays) { }
}
