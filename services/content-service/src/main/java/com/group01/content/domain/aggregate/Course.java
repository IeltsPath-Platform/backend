package com.group01.content.domain.aggregate;

import com.group01.content.domain.vo.BandRange;
import com.group01.content.domain.vo.ContentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Course {
    private final UUID id;
    private final String code;
    private String name;
    private BigDecimal bandLevel;
    private ContentStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Course(UUID id, String code, String name, BigDecimal bandLevel, ContentStatus status,
                  Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = requireText(code, "code", 100);
        this.name = requireText(name, "name", 255);
        this.bandLevel = validateBand(bandLevel);
        this.status = status != null ? status : ContentStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static Course create(String code, String name, BigDecimal bandLevel) {
        Instant now = Instant.now();
        return new Course(UUID.randomUUID(), code, name, bandLevel, ContentStatus.ACTIVE, now, now);
    }

    public void update(String name, BigDecimal bandLevel, ContentStatus status) {
        String checkedName = requireText(name, "name", 255);
        BigDecimal checkedBand = validateBand(bandLevel);
        this.name = checkedName;
        this.bandLevel = checkedBand;
        if (status != null) this.status = status;
        this.updatedAt = Instant.now();
    }

    private static BigDecimal validateBand(BigDecimal bandLevel) {
        if (bandLevel == null) throw new IllegalArgumentException("bandLevel is required");
        return BandRange.of(bandLevel, bandLevel).min();
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) throw new IllegalArgumentException(field + " exceeds " + maxLength + " characters");
        return trimmed;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public BigDecimal getBandLevel() { return bandLevel; }
    public ContentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
