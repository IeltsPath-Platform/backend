package com.group01.content.domain.aggregate;

import com.group01.content.domain.entity.VocabularySense;
import com.group01.content.domain.vo.ContentStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class VocabularyItem {
    private final UUID id;
    private String lemma;
    private String normalizedLemma;
    private String ipa;
    private String pronunciationAudioReference;
    private ContentStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<VocabularySense> senses;

    public VocabularyItem(UUID id, String lemma, String normalizedLemma, String ipa,
                          String pronunciationAudioReference, ContentStatus status,
                          Instant createdAt, Instant updatedAt, List<VocabularySense> senses) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.lemma = Objects.requireNonNull(lemma, "lemma must not be null");
        this.normalizedLemma = normalizedLemma != null ? normalizedLemma : lemma.trim().toLowerCase(Locale.ROOT);
        this.ipa = ipa;
        this.pronunciationAudioReference = pronunciationAudioReference;
        this.status = status != null ? status : ContentStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.senses = senses != null ? new ArrayList<>(senses) : new ArrayList<>();
    }

    public static VocabularyItem create(String lemma, String ipa, String pronunciationAudioReference) {
        Instant now = Instant.now();
        String normalized = lemma.trim().toLowerCase(Locale.ROOT);
        return new VocabularyItem(UUID.randomUUID(), lemma.trim(), normalized, ipa,
                pronunciationAudioReference, ContentStatus.ACTIVE, now, now, new ArrayList<>());
    }

    public void update(String lemma, String ipa, String pronunciationAudioReference, ContentStatus status) {
        this.lemma = Objects.requireNonNull(lemma, "lemma must not be null").trim();
        this.normalizedLemma = this.lemma.toLowerCase(Locale.ROOT);
        this.ipa = ipa;
        this.pronunciationAudioReference = pronunciationAudioReference;
        if (status != null) {
            this.status = status;
        }
        this.updatedAt = Instant.now();
    }

    public void addSense(VocabularySense sense) {
        Objects.requireNonNull(sense, "sense must not be null");
        this.senses.add(sense);
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getLemma() { return lemma; }
    public String getNormalizedLemma() { return normalizedLemma; }
    public String getIpa() { return ipa; }
    public String getPronunciationAudioReference() { return pronunciationAudioReference; }
    public ContentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<VocabularySense> getSenses() { return Collections.unmodifiableList(senses); }
}

