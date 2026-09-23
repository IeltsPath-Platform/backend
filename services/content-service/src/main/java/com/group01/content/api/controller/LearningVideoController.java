package com.group01.content.api.controller;

import com.group01.content.api.dto.request.AddSegmentLexicalEntryRequest;
import com.group01.content.api.dto.request.AddVideoSegmentRequest;
import com.group01.content.api.dto.request.CreateLearningVideoRequest;
import com.group01.content.api.dto.response.LearningVideoResponse;
import com.group01.content.api.dto.response.VideoDetailResponse;
import com.group01.content.api.dto.response.VideoSegmentResponse;
import com.group01.content.application.command.AddSegmentLexicalEntryCommand;
import com.group01.content.application.command.AddVideoSegmentCommand;
import com.group01.content.application.command.CreateLearningVideoCommand;
import com.group01.content.application.command.PublishLearningVideoCommand;
import com.group01.content.application.result.LearningVideoResult;
import com.group01.content.application.result.VideoDetailResult;
import com.group01.content.application.result.VideoSegmentResult;
import com.group01.content.application.usecase.*;
import com.group01.content.domain.vo.AccessLevel;
import com.group01.content.domain.vo.PublicationStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public List<LearningVideoResponse> listVideos(
            @RequestParam(value = "accessLevel", required = false) AccessLevel accessLevel,
            @RequestParam(value = "status", required = false) PublicationStatus status
    ) {
        return listLearningVideosUseCase.execute(accessLevel, status).stream()
                .map(LearningVideoResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public VideoDetailResponse getVideoDetail(@PathVariable("id") UUID id) {
        VideoDetailResult result = getLearningVideoDetailUseCase.execute(id);
        return VideoDetailResponse.from(result);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
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
                request.accessLevel(),
                null
        ));
        return LearningVideoResponse.from(result);
    }

    @PostMapping("/{id}/segments")
    @ResponseStatus(HttpStatus.CREATED)
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
    public LearningVideoResponse publishVideo(@PathVariable("id") UUID id) {
        LearningVideoResult result = publishLearningVideoUseCase.execute(new PublishLearningVideoCommand(id));
        return LearningVideoResponse.from(result);
    }
}
