package com.group01.content.application.usecase;

import com.group01.content.application.command.AddSegmentLexicalEntryCommand;
import com.group01.content.domain.aggregate.LearningVideo;
import com.group01.content.domain.entity.VideoSegment;
import com.group01.content.domain.entity.VideoSegmentLexicalEntry;
import com.group01.content.domain.exception.VideoNotFoundException;
import com.group01.content.domain.repository.LearningVideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AddSegmentLexicalEntryUseCase {

    private final LearningVideoRepository learningVideoRepository;

    public AddSegmentLexicalEntryUseCase(LearningVideoRepository learningVideoRepository) {
        this.learningVideoRepository = learningVideoRepository;
    }

    public void execute(AddSegmentLexicalEntryCommand command) {
        List<LearningVideo> allVideos = learningVideoRepository.findAll(null, null);
        LearningVideo targetVideo = null;
        VideoSegment targetSegment = null;

        for (LearningVideo v : allVideos) {
            for (VideoSegment s : v.getSegments()) {
                if (s.getId().equals(command.segmentId())) {
                    targetVideo = v;
                    targetSegment = s;
                    break;
                }
            }
            if (targetSegment != null) break;
        }

        if (targetSegment == null) {
            throw new VideoNotFoundException("Video segment not found: " + command.segmentId());
        }

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

