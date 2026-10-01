package com.group01.content.infrastructure.persistence.mapper;

import com.group01.content.domain.aggregate.LearningVideo;
import com.group01.content.domain.entity.VideoSegment;
import com.group01.content.domain.entity.VideoSegmentLexicalEntry;
import com.group01.content.infrastructure.persistence.entity.LearningVideoJpaEntity;
import com.group01.content.infrastructure.persistence.entity.VideoSegmentJpaEntity;
import com.group01.content.infrastructure.persistence.entity.VideoSegmentLexicalEntryJpaEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LearningVideoPersistenceMapper {

    public LearningVideo toDomain(LearningVideoJpaEntity entity) {
        if (entity == null) return null;
        List<VideoSegment> segments = new ArrayList<>();
        if (entity.getSegments() != null) {
            for (VideoSegmentJpaEntity s : entity.getSegments()) {
                segments.add(toSegmentDomain(s));
            }
        }
        return new LearningVideo(
                entity.getId(),
                entity.getYoutubeVideoId(),
                entity.getYoutubeUrl(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getThumbnailUrl(),
                entity.getDurationSeconds(),
                entity.getTopicId(),
                entity.getLevel(),
                entity.getRequiredFeatureKey(),
                entity.getStatus(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                segments
        );
    }

    public LearningVideoJpaEntity toEntity(LearningVideo domain) {
        if (domain == null) return null;
        List<VideoSegmentJpaEntity> segmentEntities = new ArrayList<>();
        if (domain.getSegments() != null) {
            for (VideoSegment s : domain.getSegments()) {
                segmentEntities.add(toSegmentEntity(s));
            }
        }
        return LearningVideoJpaEntity.builder()
                .id(domain.getId())
                .youtubeVideoId(domain.getYoutubeVideoId())
                .youtubeUrl(domain.getYoutubeUrl())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .thumbnailUrl(domain.getThumbnailUrl())
                .durationSeconds(domain.getDurationSeconds())
                .topicId(domain.getTopicId())
                .level(domain.getLevel())
                .requiredFeatureKey(domain.getRequiredFeatureKey())
                .status(domain.getStatus())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .segments(segmentEntities)
                .build();
    }

    public VideoSegment toSegmentDomain(VideoSegmentJpaEntity entity) {
        if (entity == null) return null;
        List<VideoSegmentLexicalEntry> lexicalEntries = new ArrayList<>();
        if (entity.getLexicalEntries() != null) {
            for (VideoSegmentLexicalEntryJpaEntity l : entity.getLexicalEntries()) {
                lexicalEntries.add(toLexicalEntryDomain(l));
            }
        }
        return new VideoSegment(
                entity.getId(),
                entity.getVideoId(),
                entity.getSequenceNo(),
                entity.getStartMs(),
                entity.getEndMs(),
                entity.getTranscript(),
                entity.getTranslationVi(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                lexicalEntries
        );
    }

    public VideoSegmentJpaEntity toSegmentEntity(VideoSegment domain) {
        if (domain == null) return null;
        List<VideoSegmentLexicalEntryJpaEntity> lexicalEntities = new ArrayList<>();
        if (domain.getLexicalEntries() != null) {
            for (VideoSegmentLexicalEntry l : domain.getLexicalEntries()) {
                lexicalEntities.add(toLexicalEntryEntity(l));
            }
        }
        return VideoSegmentJpaEntity.builder()
                .id(domain.getId())
                .videoId(domain.getVideoId())
                .sequenceNo(domain.getSequenceNo())
                .startMs(domain.getStartMs())
                .endMs(domain.getEndMs())
                .transcript(domain.getTranscript())
                .translationVi(domain.getTranslationVi())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .lexicalEntries(lexicalEntities)
                .build();
    }

    public VideoSegmentLexicalEntry toLexicalEntryDomain(VideoSegmentLexicalEntryJpaEntity entity) {
        if (entity == null) return null;
        return new VideoSegmentLexicalEntry(
                entity.getId(),
                entity.getSegmentId(),
                entity.getVocabularySenseId(),
                entity.getSurfaceText(),
                entity.getStartChar(),
                entity.getEndChar(),
                entity.getSortOrder(),
                entity.getCreatedAt()
        );
    }

    public VideoSegmentLexicalEntryJpaEntity toLexicalEntryEntity(VideoSegmentLexicalEntry domain) {
        if (domain == null) return null;
        return VideoSegmentLexicalEntryJpaEntity.builder()
                .id(domain.getId())
                .segmentId(domain.getSegmentId())
                .vocabularySenseId(domain.getVocabularySenseId())
                .surfaceText(domain.getSurfaceText())
                .startChar(domain.getStartChar())
                .endChar(domain.getEndChar())
                .sortOrder(domain.getSortOrder())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
