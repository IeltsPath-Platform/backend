package com.group01.content.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class VideoSegmentLexicalEntry {
    private final UUID id;
    private final UUID segmentId;
    private UUID vocabularySenseId;
    private String surfaceText;
    private int startChar;
    private int endChar;
    private int sortOrder;
    private final Instant createdAt;

    public VideoSegmentLexicalEntry(UUID id, UUID segmentId, UUID vocabularySenseId,
                                   String surfaceText, int startChar, int endChar,
                                   int sortOrder, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.segmentId = Objects.requireNonNull(segmentId, "segmentId must not be null");
        this.vocabularySenseId = vocabularySenseId;
        this.surfaceText = Objects.requireNonNull(surfaceText, "surfaceText must not be null");
        if (startChar < 0 || endChar <= startChar) {
            throw new IllegalArgumentException("Invalid startChar/endChar bounds: " + startChar + ", " + endChar);
        }
        this.startChar = startChar;
        this.endChar = endChar;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static VideoSegmentLexicalEntry create(UUID segmentId, UUID vocabularySenseId,
                                                  String surfaceText, int startChar, int endChar, int sortOrder) {
        return new VideoSegmentLexicalEntry(UUID.randomUUID(), segmentId, vocabularySenseId,
                surfaceText, startChar, endChar, sortOrder, Instant.now());
    }

    public UUID getId() { return id; }
    public UUID getSegmentId() { return segmentId; }
    public UUID getVocabularySenseId() { return vocabularySenseId; }
    public String getSurfaceText() { return surfaceText; }
    public int getStartChar() { return startChar; }
    public int getEndChar() { return endChar; }
    public int getSortOrder() { return sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
}

