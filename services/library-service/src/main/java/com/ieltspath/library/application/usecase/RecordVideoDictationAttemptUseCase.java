package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.command.RecordVideoDictationAttemptCommand;
import com.ieltspath.library.application.result.VideoDictationAttemptResult;
import com.ieltspath.library.domain.aggregate.VideoDictationAttempt;
import com.ieltspath.library.domain.exception.ResourceNotFoundException;
import com.ieltspath.library.domain.repository.DictationVideoRepository;
import com.ieltspath.library.domain.repository.VideoDictationAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Stores one dictation attempt. The video is taken from the segment, never from the client, and the attempt number is
 * counted here so two tabs cannot claim the same number (the unique constraint turns a race into a conflict).
 */
@Service
@RequiredArgsConstructor
public class RecordVideoDictationAttemptUseCase {
    private final DictationVideoRepository videoRepository;
    private final VideoDictationAttemptRepository attemptRepository;

    @Transactional
    public VideoDictationAttemptResult execute(RecordVideoDictationAttemptCommand command) {
        UUID videoId = videoRepository.findPublishedVideoIdBySegmentId(command.segmentId())
                .orElseThrow(ResourceNotFoundException::new);
        int attemptNo = attemptRepository.maxAttemptNo(command.userId(), command.segmentId()) + 1;
        return VideoDictationAttemptResult.from(attemptRepository.save(VideoDictationAttempt.create(
                command.userId(),
                videoId,
                command.segmentId(),
                attemptNo,
                command.typedText(),
                command.correct(),
                command.accuracyPercent()
        )));
    }
}
