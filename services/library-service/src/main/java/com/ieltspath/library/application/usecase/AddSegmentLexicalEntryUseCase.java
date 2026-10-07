package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.command.AddSegmentLexicalEntryCommand;
import com.ieltspath.library.domain.entity.VideoSegment;
import com.ieltspath.library.domain.entity.VideoSegmentLexicalEntry;
import com.ieltspath.library.domain.exception.VideoNotFoundException;
import com.ieltspath.library.domain.repository.LearningVideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddSegmentLexicalEntryUseCase {

    private final LearningVideoRepository learningVideoRepository;

    public AddSegmentLexicalEntryUseCase(LearningVideoRepository learningVideoRepository) {
        this.learningVideoRepository = learningVideoRepository;
    }

    public void execute(AddSegmentLexicalEntryCommand command) {
        var targetVideo = learningVideoRepository.findBySegmentId(command.segmentId())
                .orElseThrow(() -> new VideoNotFoundException(
                        "Video segment not found: " + command.segmentId()));
        VideoSegment targetSegment = targetVideo.getSegments().stream()
                .filter(segment -> segment.getId().equals(command.segmentId()))
                .findFirst()
                .orElseThrow(() -> new VideoNotFoundException(
                        "Video segment not found: " + command.segmentId()));

        VideoSegmentLexicalEntry entry = VideoSegmentLexicalEntry.create(
                command.segmentId(),
                command.vocabularySenseId(),
                command.surfaceText(),
                command.startChar(),
                command.endChar(),
                command.sortOrder()
        );

        targetSegment.addLexicalEntry(entry);
        learningVideoRepository.save(targetVideo);
    }
}
