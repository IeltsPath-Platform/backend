package com.ieltspath.library.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.library.api.dto.request.RecordVideoDictationAttemptRequest;
import com.ieltspath.library.api.dto.response.DictationVideoDetailResponse;
import com.ieltspath.library.api.dto.response.DictationVideoListItemResponse;
import com.ieltspath.library.api.dto.response.VideoDictationAttemptResponse;
import com.ieltspath.library.api.dto.response.VideoDictationProgressResponse;
import com.ieltspath.library.application.command.RecordVideoDictationAttemptCommand;
import com.ieltspath.library.application.usecase.GetDictationVideoUseCase;
import com.ieltspath.library.application.usecase.GetVideoDictationProgressUseCase;
import com.ieltspath.library.application.usecase.ListDictationVideosUseCase;
import com.ieltspath.library.application.usecase.RecordVideoDictationAttemptUseCase;
import com.ieltspath.library.domain.vo.VideoLevel;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Video dictation. The frontend plays the YouTube video, pauses at each segment end, compares the typed text with the
 * segment transcript and sends its verdict. The backend lists videos, serves the sentences, stores attempts and
 * reports progress. Only published videos are visible, and all of them are open to every learner.
 *
 * <p>Request parameters and path variables carry explicit names: the module is not compiled with {@code -parameters},
 * so an unnamed one fails at runtime with an IllegalArgumentException that the handler turns into a 400.
 */
@RestController
@RequestMapping("/api/learning-support/video-dictation")
@RequiredArgsConstructor
public class VideoDictationController {
    private final CurrentUserProvider currentUserProvider;
    private final ListDictationVideosUseCase listVideosUseCase;
    private final GetDictationVideoUseCase getVideoUseCase;
    private final RecordVideoDictationAttemptUseCase recordAttemptUseCase;
    private final GetVideoDictationProgressUseCase getProgressUseCase;

    /** Library: published videos with the learner's passed / total sentences; {@code level} filters one level. */
    @GetMapping("/videos")
    public List<DictationVideoListItemResponse> list(@RequestParam(name = "level", required = false) VideoLevel level) {
        return listVideosUseCase.execute(currentUserProvider.requireUserId(), level).stream()
                .map(DictationVideoListItemResponse::from)
                .toList();
    }

    /** One video with its sentences (start / end in ms, transcript, Vietnamese translation). */
    @GetMapping("/videos/{videoId}")
    public DictationVideoDetailResponse video(@PathVariable("videoId") UUID videoId) {
        return DictationVideoDetailResponse.from(getVideoUseCase.execute(videoId));
    }

    /** Stores one typed attempt; the server numbers it and takes the video from the segment. */
    @PostMapping("/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public VideoDictationAttemptResponse record(@Valid @RequestBody RecordVideoDictationAttemptRequest request) {
        return VideoDictationAttemptResponse.from(recordAttemptUseCase.execute(new RecordVideoDictationAttemptCommand(
                currentUserProvider.requireUserId(),
                request.segmentId(),
                request.typedText(),
                request.correct(),
                request.accuracyPercent()
        )));
    }

    /** Per-sentence progress of the learner in one video, plus the first sentence not passed yet. */
    @GetMapping("/videos/{videoId}/progress")
    public VideoDictationProgressResponse progress(@PathVariable("videoId") UUID videoId) {
        return VideoDictationProgressResponse.from(getProgressUseCase.execute(currentUserProvider.requireUserId(), videoId));
    }
}
