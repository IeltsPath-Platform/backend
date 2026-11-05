package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.vo.BandRange;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.Skill;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class Course {
    private final UUID id;
    private final String code;
    private String name;
    private BigDecimal bandLevel;
    private String coverImageUrl;
    /** The wide image behind the course page header; the course page falls back to the cover image without one. */
    private String bannerImageUrl;
    private String description;
    private Map<Skill, Integer> skillUnlockPoints;
    private Integer comboUnlockPoints;
    /** A free course is open to everyone: none of its lessons or points sits behind the course paywall. */
    private boolean free;
    private ContentStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Course(UUID id, String code, String name, BigDecimal bandLevel, ContentStatus status,
                  Instant createdAt, Instant updatedAt) {
        this(id, code, name, bandLevel, status, createdAt, updatedAt, null, null);
    }

    public Course(UUID id, String code, String name, BigDecimal bandLevel, ContentStatus status,
                  Instant createdAt, Instant updatedAt, String coverImageUrl, String description) {
        this(id, code, name, bandLevel, status, createdAt, updatedAt, coverImageUrl, description, Map.of(), null);
    }

    public Course(UUID id, String code, String name, BigDecimal bandLevel, ContentStatus status,
                  Instant createdAt, Instant updatedAt, String coverImageUrl, String description,
                  Map<Skill, Integer> skillUnlockPoints, Integer comboUnlockPoints) {
        this(id, code, name, bandLevel, status, createdAt, updatedAt, coverImageUrl, description, skillUnlockPoints,
                comboUnlockPoints, false);
    }

    public Course(UUID id, String code, String name, BigDecimal bandLevel, ContentStatus status,
                  Instant createdAt, Instant updatedAt, String coverImageUrl, String description,
                  Map<Skill, Integer> skillUnlockPoints, Integer comboUnlockPoints, boolean free) {
        this(id, code, name, bandLevel, status, createdAt, updatedAt, coverImageUrl, description, skillUnlockPoints,
                comboUnlockPoints, free, null);
    }

    public Course(UUID id, String code, String name, BigDecimal bandLevel, ContentStatus status,
                  Instant createdAt, Instant updatedAt, String coverImageUrl, String description,
                  Map<Skill, Integer> skillUnlockPoints, Integer comboUnlockPoints, boolean free,
                  String bannerImageUrl) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = requireText(code, "code", 100);
        this.name = requireText(name, "name", 255);
        this.bandLevel = validateBand(bandLevel);
        this.coverImageUrl = normalizeCoverImageUrl(coverImageUrl);
        this.bannerImageUrl = normalizeCoverImageUrl(bannerImageUrl);
        this.description = normalizeOptionalText(description, "description", 2000);
        this.skillUnlockPoints = validatePrices(skillUnlockPoints, comboUnlockPoints);
        this.comboUnlockPoints = comboUnlockPoints;
        this.free = free;
        requireNoPricesWhenFree(this.free, this.skillUnlockPoints, this.comboUnlockPoints);
        this.status = status != null ? status : ContentStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static Course create(String code, String name, BigDecimal bandLevel) {
        return create(code, name, bandLevel, null, null);
    }

    public static Course create(String code, String name, BigDecimal bandLevel, String coverImageUrl,
                                String description) {
        return create(code, name, bandLevel, coverImageUrl, description, Map.of(), null);
    }

    public static Course create(String code, String name, BigDecimal bandLevel, String coverImageUrl,
                                String description, Map<Skill, Integer> skillUnlockPoints,
                                Integer comboUnlockPoints) {
        return create(code, name, bandLevel, coverImageUrl, description, skillUnlockPoints, comboUnlockPoints, false);
    }

    public static Course create(String code, String name, BigDecimal bandLevel, String coverImageUrl,
                                String description, Map<Skill, Integer> skillUnlockPoints,
                                Integer comboUnlockPoints, boolean free) {
        return create(code, name, bandLevel, coverImageUrl, description, skillUnlockPoints, comboUnlockPoints, free, null);
    }

    public static Course create(String code, String name, BigDecimal bandLevel, String coverImageUrl,
                                String description, Map<Skill, Integer> skillUnlockPoints,
                                Integer comboUnlockPoints, boolean free, String bannerImageUrl) {
        Instant now = Instant.now();
        return new Course(UUID.randomUUID(), code, name, bandLevel, ContentStatus.ACTIVE, now, now,
                coverImageUrl, description, skillUnlockPoints, comboUnlockPoints, free, bannerImageUrl);
    }

    public void update(String name, BigDecimal bandLevel, ContentStatus status) {
        update(name, bandLevel, status, coverImageUrl, description);
    }

    public void update(String name, BigDecimal bandLevel, ContentStatus status, String coverImageUrl,
                       String description) {
        update(name, bandLevel, status, coverImageUrl, description, null, null);
    }

    public void update(String name, BigDecimal bandLevel, ContentStatus status, String coverImageUrl,
                       String description, Map<Skill, Integer> skillUnlockPoints, Integer comboUnlockPoints) {
        update(name, bandLevel, status, coverImageUrl, description, skillUnlockPoints, comboUnlockPoints, null);
    }

    /** {@code free} left null keeps the course as it is; the banner image stays as it is too. */
    public void update(String name, BigDecimal bandLevel, ContentStatus status, String coverImageUrl,
                       String description, Map<Skill, Integer> skillUnlockPoints, Integer comboUnlockPoints,
                       Boolean free) {
        update(name, bandLevel, status, coverImageUrl, description, skillUnlockPoints, comboUnlockPoints, free,
                this.bannerImageUrl);
    }

    /** Like the cover image, the banner image is replaced by whatever is given: null removes it. */
    public void update(String name, BigDecimal bandLevel, ContentStatus status, String coverImageUrl,
                       String description, Map<Skill, Integer> skillUnlockPoints, Integer comboUnlockPoints,
                       Boolean free, String bannerImageUrl) {
        String checkedName = requireText(name, "name", 255);
        BigDecimal checkedBand = validateBand(bandLevel);
        String checkedCoverImageUrl = normalizeCoverImageUrl(coverImageUrl);
        String checkedBannerImageUrl = normalizeCoverImageUrl(bannerImageUrl);
        String checkedDescription = normalizeOptionalText(description, "description", 2000);
        Map<Skill, Integer> checkedPrices = skillUnlockPoints == null
                ? this.skillUnlockPoints
                : validatePrices(skillUnlockPoints, comboUnlockPoints);
        boolean checkedFree = free != null ? free : this.free;
        Integer checkedCombo = skillUnlockPoints != null ? comboUnlockPoints : this.comboUnlockPoints;
        requireNoPricesWhenFree(checkedFree, checkedPrices, checkedCombo);
        this.name = checkedName;
        this.bandLevel = checkedBand;
        this.coverImageUrl = checkedCoverImageUrl;
        this.bannerImageUrl = checkedBannerImageUrl;
        this.description = checkedDescription;
        this.skillUnlockPoints = checkedPrices;
        this.comboUnlockPoints = checkedCombo;
        this.free = checkedFree;
        if (status != null) this.status = status;
        this.updatedAt = Instant.now();
    }

    private static void requireNoPricesWhenFree(boolean free, Map<Skill, Integer> prices, Integer combo) {
        if (free && (!prices.isEmpty() || combo != null)) {
            throw new IllegalArgumentException("a free course has no unlock prices");
        }
    }

    private static Map<Skill, Integer> validatePrices(Map<Skill, Integer> prices, Integer combo) {
        EnumMap<Skill, Integer> normalized = new EnumMap<>(Skill.class);
        if (prices != null) prices.forEach((skill, price) -> {
            if (skill == null || skill == Skill.ALL) throw new IllegalArgumentException("unknown skill price");
            if (price == null) return;
            if (price <= 0) throw new IllegalArgumentException("skill unlock prices must be positive integers");
            normalized.put(skill, price);
        });
        if (combo != null && combo <= 0) throw new IllegalArgumentException("combo unlock price must be a positive integer");
        if (combo != null && !normalized.isEmpty()) {
            int highest = normalized.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            if (combo < highest) throw new IllegalArgumentException("combo unlock price must be at least the highest skill price");
            if (normalized.size() >= 2) {
                long sum = normalized.values().stream().mapToLong(Integer::longValue).sum();
                if (combo > sum) throw new IllegalArgumentException("combo unlock price must not exceed the sum of skill prices");
            }
        }
        return Map.copyOf(normalized);
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

    private static String normalizeCoverImageUrl(String value) {
        String normalized = normalizeOptionalText(value, "coverImageUrl", 500);
        if (normalized != null && normalized.startsWith("content-image/")) {
            if (normalized.contains("..") || !normalized.matches("^content-image/[A-Za-z0-9._/-]+$")) {
                throw new IllegalArgumentException("coverImageUrl must be a safe content image key");
            }
            return normalized;
        }
        if (normalized != null && !normalized.matches("(?i)^https?://.*")) {
            throw new IllegalArgumentException("coverImageUrl must be an http(s) URL or content image key");
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(field + " exceeds " + maxLength + " characters");
        }
        return trimmed;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public BigDecimal getBandLevel() { return bandLevel; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public String getBannerImageUrl() { return bannerImageUrl; }
    public String getDescription() { return description; }
    public Map<Skill, Integer> getSkillUnlockPoints() { return skillUnlockPoints; }
    public Integer getComboUnlockPoints() { return comboUnlockPoints; }
    public boolean isFree() { return free; }
    public ContentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
