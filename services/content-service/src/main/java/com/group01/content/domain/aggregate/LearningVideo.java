package com.group01.content.domain.aggregate;

import com.group01.content.domain.entity.VideoSegment;
import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.domain.vo.VideoLevel;

import java.time.Instant;
import java.util.*;

public class LearningVideo {
    private final UUID id;
    private String youtubeVideoId;
    private String youtubeUrl;
    private String title;
    private String description;
    private String thumbnailUrl;
    private Integer durationSeconds;
    private UUID topicId;
    private VideoLevel level;
    private String requiredFeatureKey;
    private PublicationStatus status;
    private UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<VideoSegment> segments;

    public LearningVideo(UUID id, String youtubeVideoId, String youtubeUrl, String title,
                         String description, String thumbnailUrl, Integer durationSeconds,
                         UUID topicId, VideoLevel level, String requiredFeatureKey,
                         PublicationStatus status, UUID createdBy,
                         Instant createdAt, Instant updatedAt, List<VideoSegment> segments) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.youtubeVideoId = Objects.requireNonNull(youtubeVideoId, "youtubeVideoId must not be null");
        this.youtubeUrl = Objects.requireNonNull(youtubeUrl, "youtubeUrl must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.durationSeconds = durationSeconds;
        this.topicId = topicId;
        this.level = level;
        this.requiredFeatureKey = requiredFeatureKey;
        this.status = status != null ? status : PublicationStatus.DRAFT;
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.segments = segments != null ? new ArrayList<>(segments) : new ArrayList<>();
    }

    public static LearningVideo create(String youtubeVideoId, String youtubeUrl, String title,
                                       String description, String thumbnailUrl, Integer durationSeconds,
                                       UUID topicId, VideoLevel level, String requiredFeatureKey, UUID createdBy) {
        Instant now = Instant.now();
        return new LearningVideo(UUID.randomUUID(), youtubeVideoId, youtubeUrl, title, description,
                thumbnailUrl, durationSeconds, topicId, level, requiredFeatureKey,
                PublicationStatus.DRAFT, createdBy, now, now, new ArrayList<>());
    }

    public void publish() {
        this.status = PublicationStatus.PUBLISHED;
        this.updatedAt = Instant.now();
    }

    public void addSegment(VideoSegment segment) {
        this.segments.add(Objects.requireNonNull(segment, "segment must not be null"));
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getYoutubeVideoId() { return youtubeVideoId; }
    public String getYoutubeUrl() { return youtubeUrl; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public UUID getTopicId() { return topicId; }
    public VideoLevel getLevel() { return level; }

    public String getRequiredFeatureKey() {
        return requiredFeatureKey;
    }
    public PublicationStatus getStatus() { return status; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<VideoSegment> getSegments() { return Collections.unmodifiableList(segments); }
}
