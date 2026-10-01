package com.group01.content.domain.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class VideoSegment {
    private final UUID id;
    private final UUID videoId;
    private int sequenceNo;
    private int startMs;
    private int endMs;
    private String transcript;
    private String translationVi;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<VideoSegmentLexicalEntry> lexicalEntries;

    public VideoSegment(UUID id, UUID videoId, int sequenceNo, int startMs, int endMs,
                        String transcript, String translationVi, Instant createdAt, Instant updatedAt,
                        List<VideoSegmentLexicalEntry> lexicalEntries) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.videoId = Objects.requireNonNull(videoId, "videoId must not be null");
        if (sequenceNo <= 0) {
            throw new IllegalArgumentException("sequenceNo must be greater than 0");
        }
        if (startMs < 0 || endMs <= startMs) {
            throw new IllegalArgumentException("Invalid segment timing: startMs=" + startMs + ", endMs=" + endMs);
        }
        this.sequenceNo = sequenceNo;
        this.startMs = startMs;
        this.endMs = endMs;
        this.transcript = Objects.requireNonNull(transcript, "transcript must not be null");
        this.translationVi = translationVi;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.lexicalEntries = lexicalEntries != null ? new ArrayList<>(lexicalEntries) : new ArrayList<>();
    }

    public static VideoSegment create(UUID videoId, int sequenceNo, int startMs, int endMs,
                                      String transcript, String translationVi) {
        Instant now = Instant.now();
        return new VideoSegment(UUID.randomUUID(), videoId, sequenceNo, startMs, endMs,
                transcript, translationVi, now, now, new ArrayList<>());
    }

    public void addLexicalEntry(VideoSegmentLexicalEntry entry) {
        this.lexicalEntries.add(Objects.requireNonNull(entry, "entry must not be null"));
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getVideoId() { return videoId; }
    public int getSequenceNo() { return sequenceNo; }
    public int getStartMs() { return startMs; }
    public int getEndMs() { return endMs; }
    public String getTranscript() { return transcript; }
    public String getTranslationVi() { return translationVi; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<VideoSegmentLexicalEntry> getLexicalEntries() { return Collections.unmodifiableList(lexicalEntries); }
}

