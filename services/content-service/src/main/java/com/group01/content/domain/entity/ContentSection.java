package com.group01.content.domain.entity;

import com.group01.content.domain.vo.Skill;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ContentSection {
    private final UUID id;
    private final UUID packageVersionId;
    private String title;
    private Skill skill;
    private int sortOrder;
    private Integer timeLimitSeconds;
    private String instructions;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<SectionQuestion> questions;

    public ContentSection(UUID id, UUID packageVersionId, String title, Skill skill,
                          int sortOrder, Integer timeLimitSeconds, String instructions,
                          Instant createdAt, Instant updatedAt, List<SectionQuestion> questions) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.packageVersionId = Objects.requireNonNull(packageVersionId, "packageVersionId must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.skill = skill;
        this.sortOrder = sortOrder;
        this.timeLimitSeconds = timeLimitSeconds;
        this.instructions = instructions;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.questions = questions != null ? new ArrayList<>(questions) : new ArrayList<>();
    }

    public static ContentSection create(UUID packageVersionId, String title, Skill skill,
                                        int sortOrder, Integer timeLimitSeconds, String instructions) {
        Instant now = Instant.now();
        return new ContentSection(UUID.randomUUID(), packageVersionId, title, skill,
                sortOrder, timeLimitSeconds, instructions, now, now, new ArrayList<>());
    }

    public void addQuestion(SectionQuestion sectionQuestion) {
        this.questions.add(Objects.requireNonNull(sectionQuestion, "sectionQuestion must not be null"));
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPackageVersionId() { return packageVersionId; }
    public String getTitle() { return title; }
    public Skill getSkill() { return skill; }
    public int getSortOrder() { return sortOrder; }
    public Integer getTimeLimitSeconds() { return timeLimitSeconds; }
    public String getInstructions() { return instructions; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<SectionQuestion> getQuestions() { return Collections.unmodifiableList(questions); }
}

