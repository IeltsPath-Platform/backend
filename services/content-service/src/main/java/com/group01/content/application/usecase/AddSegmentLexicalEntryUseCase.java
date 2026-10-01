package com.group01.content.application.usecase;

import com.group01.content.application.command.AddSegmentLexicalEntryCommand;
import com.group01.content.domain.entity.VideoSegment;
import com.group01.content.domain.entity.VideoSegmentLexicalEntry;
import com.group01.content.domain.exception.VideoNotFoundException;
import com.group01.content.domain.repository.LearningVideoRepository;
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
