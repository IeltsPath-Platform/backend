package com.group01.library.application.usecase;

import com.group01.library.application.command.AddVideoSegmentCommand;
import com.group01.library.application.result.VideoSegmentResult;
import com.group01.library.domain.aggregate.LearningVideo;
import com.group01.library.domain.entity.VideoSegment;
import com.group01.library.domain.exception.VideoNotFoundException;
import com.group01.library.domain.repository.LearningVideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AddVideoSegmentUseCase {

    private final LearningVideoRepository learningVideoRepository;

    public AddVideoSegmentUseCase(LearningVideoRepository learningVideoRepository) {
        this.learningVideoRepository = learningVideoRepository;
    }

    public VideoSegmentResult execute(AddVideoSegmentCommand command) {
        LearningVideo video = learningVideoRepository.findById(command.videoId())
                .orElseThrow(() -> new VideoNotFoundException(command.videoId()));

        VideoSegment segment = VideoSegment.create(
                command.videoId(),
                command.sequenceNo(),
                command.startMs(),
                command.endMs(),
                command.transcript(),
                command.translationVi()
        );

        video.addSegment(segment);
        learningVideoRepository.save(video);

        return new VideoSegmentResult(
                segment.getId(),
                segment.getVideoId(),
                segment.getSequenceNo(),
                segment.getStartMs(),
                segment.getEndMs(),
                segment.getTranscript(),
                segment.getTranslationVi(),
                segment.getCreatedAt(),
                segment.getUpdatedAt(),
                List.of()
        );
    }
}
