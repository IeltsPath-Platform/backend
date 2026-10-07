package com.group01.content.domain.aggregate;

import com.group01.content.domain.vo.BandRange;
import com.group01.content.domain.vo.ContentStatus;
import com.group01.content.domain.vo.Skill;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Topic {
    private final UUID id;
    private UUID parentTopicId;
    private UUID courseId;
    private String code;
    private String name;
    private int sortOrder;
    private ContentStatus status;
    private BandRange band;
    private Skill skill;
    private final Instant createdAt;
    private Instant updatedAt;

    public Topic(UUID id, UUID parentTopicId, String code, String name, int sortOrder,
                 ContentStatus status, BandRange band, Skill skill, Instant createdAt, Instant updatedAt) {
        this(id, parentTopicId, code, name, sortOrder, status, band, skill, createdAt, updatedAt, null);
    }

    public Topic(UUID id, UUID parentTopicId, String code, String name, int sortOrder,
                 ContentStatus status, BandRange band, Skill skill, Instant createdAt, Instant updatedAt, UUID courseId) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.parentTopicId = parentTopicId;
        this.courseId = courseId;
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.code = code.trim();
        this.name = name.trim();
        this.sortOrder = sortOrder;
        this.status = status != null ? status : ContentStatus.ACTIVE;
        this.band = band != null ? band : BandRange.UNBOUNDED;
        this.skill = requireSingleSkill(skill);
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static Topic create(UUID parentTopicId, String code, String name, int sortOrder) {
        return create(parentTopicId, code, name, sortOrder, BandRange.UNBOUNDED, null);
    }

    public static Topic create(UUID parentTopicId, String code, String name, int sortOrder, BandRange band,
                               Skill skill) {
        Instant now = Instant.now();
        return new Topic(UUID.randomUUID(), parentTopicId, code, name, sortOrder, ContentStatus.ACTIVE, band, skill,
                now, now);
    }

    /** Replaces every editable field, band included: a null band clears it. The skill is changed separately. */
    public void update(UUID parentTopicId, String name, int sortOrder, ContentStatus status, BandRange band) {
        this.parentTopicId = parentTopicId;
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.sortOrder = sortOrder;
        this.band = band != null ? band : BandRange.UNBOUNDED;
        if (status != null) {
            this.status = status;
        }
        this.updatedAt = Instant.now();
    }

    /**
     * Sets the topic's skill label. Learning paths read the skills of a topic's lessons, so the label may change at
     * any time; it must still name exactly one skill.
     */
    public void changeSkill(Skill newSkill) {
        Skill checked = requireSingleSkill(newSkill);
        if (checked == skill) {
            return;
        }
        this.skill = checked;
        this.updatedAt = Instant.now();
    }

    private static Skill requireSingleSkill(Skill skill) {
        if (skill == Skill.ALL) {
            throw new IllegalArgumentException("A topic teaches one skill; ALL is not allowed");
        }
        return skill;
    }

    public UUID getId() { return id; }
    public UUID getParentTopicId() { return parentTopicId; }
    public UUID getCourseId() { return courseId; }

    public void assignCourse(UUID courseId) {
        this.courseId = Objects.requireNonNull(courseId, "courseId must not be null");
        this.updatedAt = Instant.now();
    }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getSortOrder() { return sortOrder; }
    public ContentStatus getStatus() { return status; }
    public BandRange getBand() { return band; }
    public Skill getSkill() { return skill; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
