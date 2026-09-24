package com.group01.content.application.usecase;

import com.group01.content.application.result.VideoDetailResult;
import com.group01.content.application.result.VideoSegmentResult;
import com.group01.content.domain.aggregate.LearningVideo;
import com.group01.content.domain.exception.VideoNotFoundException;
import com.group01.content.domain.repository.LearningVideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetLearningVideoDetailUseCase {

    private final LearningVideoRepository learningVideoRepository;

    public GetLearningVideoDetailUseCase(LearningVideoRepository learningVideoRepository) {
        this.learningVideoRepository = learningVideoRepository;
    }

    public VideoDetailResult execute(UUID id) {
        LearningVideo video = learningVideoRepository.findById(id)
                .orElseThrow(() -> new VideoNotFoundException(id));

        List<VideoSegmentResult> segmentResults = video.getSegments().stream()
                .map(s -> new VideoSegmentResult(
                        s.getId(),
                        s.getVideoId(),
                        s.getSequenceNo(),
                        s.getStartMs(),
                        s.getEndMs(),
                        s.getTranscript(),
                        s.getTranslationVi(),
                        s.getCreatedAt(),
                        s.getUpdatedAt(),
                        s.getLexicalEntries().stream()
                                .map(entry -> new VideoSegmentLexicalEntryResult(
                                        entry.getId(),
                                        entry.getSegmentId(),
                                        entry.getVocabularySenseId(),
                                        entry.getSurfaceText(),
                                        entry.getStartChar(),
                                        entry.getEndChar(),
                                        entry.getSortOrder(),
                                        entry.getCreatedAt()
                                ))
                                .toList()
                ))
                .toList();

        return new VideoDetailResult(
                video.getId(),
                video.getYoutubeVideoId(),
                video.getYoutubeUrl(),
                video.getTitle(),
                video.getDescription(),
                video.getThumbnailUrl(),
                video.getDurationSeconds(),
                video.getTopicId(),
                video.getLevel(),
                video.getRequiredFeatureKey(),
                video.getStatus(),
                video.getCreatedBy(),
                video.getCreatedAt(),
                video.getUpdatedAt(),
                segmentResults
        );
    }
}
