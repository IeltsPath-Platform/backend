package com.ieltspath.library.api.controller;

import com.ieltspath.library.api.AccessLevelCompatibility;
import com.ieltspath.library.api.dto.AccessLevel;
import com.ieltspath.library.api.dto.request.AddSegmentLexicalEntryRequest;
import com.ieltspath.library.api.dto.request.AddVideoSegmentRequest;
import com.ieltspath.library.api.dto.request.CreateLearningVideoRequest;
import com.ieltspath.library.api.dto.response.LearningVideoResponse;
import com.ieltspath.library.api.dto.response.VideoDetailResponse;
import com.ieltspath.library.api.dto.response.VideoSegmentResponse;
import com.ieltspath.library.application.command.AddSegmentLexicalEntryCommand;
import com.ieltspath.library.application.command.AddVideoSegmentCommand;
import com.ieltspath.library.application.command.CreateLearningVideoCommand;
import com.ieltspath.library.application.command.PublishLearningVideoCommand;
import com.ieltspath.library.application.result.LearningVideoResult;
import com.ieltspath.library.application.result.VideoDetailResult;
import com.ieltspath.library.application.result.VideoSegmentResult;
import com.ieltspath.library.application.usecase.*;
import com.ieltspath.library.domain.vo.PublicationStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/content/videos")
@RequiredArgsConstructor
public class LearningVideoController {

    private final ListLearningVideosUseCase listLearningVideosUseCase;
    private final GetLearningVideoDetailUseCase getLearningVideoDetailUseCase;
    private final CreateLearningVideoUseCase createLearningVideoUseCase;
    private final AddVideoSegmentUseCase addVideoSegmentUseCase;
    private final AddSegmentLexicalEntryUseCase addSegmentLexicalEntryUseCase;
    private final PublishLearningVideoUseCase publishLearningVideoUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public List<LearningVideoResponse> listVideos(
            @RequestParam(value = "accessLevel", required = false) AccessLevel accessLevel,
            @RequestParam(value = "status", required = false) PublicationStatus status
    ) {
        return listLearningVideosUseCase.execute(
                        AccessLevelCompatibility.toFeatureRequiredFilter(accessLevel), status
                ).stream()
                .map(LearningVideoResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public VideoDetailResponse getVideoDetail(@PathVariable("id") UUID id) {
        VideoDetailResult result = getLearningVideoDetailUseCase.execute(id);
        return VideoDetailResponse.from(result);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public LearningVideoResponse createVideo(@Valid @RequestBody CreateLearningVideoRequest request) {
        LearningVideoResult result = createLearningVideoUseCase.execute(new CreateLearningVideoCommand(
                request.youtubeVideoId(),
                request.youtubeUrl(),
                request.title(),
                request.description(),
                request.thumbnailUrl(),
                request.durationSeconds(),
                request.topicId(),
                request.level(),
                AccessLevelCompatibility.toRequiredFeatureKey(request.accessLevel(), "VIDEO_LEARNING_PREMIUM"),
                null
        ));
        return LearningVideoResponse.from(result);
    }

    @PostMapping("/{id}/segments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public VideoSegmentResponse addSegment(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AddVideoSegmentRequest request
    ) {
        VideoSegmentResult result = addVideoSegmentUseCase.execute(new AddVideoSegmentCommand(
                id,
                request.sequenceNo(),
                request.startMs(),
                request.endMs(),
                request.transcript(),
                request.translationVi()
        ));
        return VideoSegmentResponse.from(result);
    }

    @PostMapping("/segments/{segmentId}/lexical-entries")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public void addSegmentLexicalEntry(
            @PathVariable("segmentId") UUID segmentId,
            @Valid @RequestBody AddSegmentLexicalEntryRequest request
    ) {
        addSegmentLexicalEntryUseCase.execute(new AddSegmentLexicalEntryCommand(
                segmentId,
                request.vocabularySenseId(),
                request.surfaceText(),
                request.startChar(),
                request.endChar(),
                request.sortOrder()
        ));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public LearningVideoResponse publishVideo(@PathVariable("id") UUID id) {
        LearningVideoResult result = publishLearningVideoUseCase.execute(new PublishLearningVideoCommand(id));
        return LearningVideoResponse.from(result);
    }
}
