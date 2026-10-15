package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Which practice sets a review may draw its questions from. A review shows the answers of its set, so a set bound to
 * the review's own lesson is never used: the lesson requires the learner to practise it, and once its answers were
 * seen it would no longer count. Only sets without that lesson are review material.
 */
@Component
public class ReviewMaterial {
    private final LearningContentClient content;

    public ReviewMaterial(LearningContentClient content) {
        this.content = content;
    }

    /** {@code excluded} plus every set bound to {@code lessonId}, in a stable order. */
    public List<UUID> excluding(UUID lessonId, Collection<UUID> excluded) {
        Set<UUID> all = new HashSet<>(excluded);
        var own = content.lessonPracticeSets(lessonId);
        if (own != null) own.forEach(set -> all.add(set.packageId()));
        return all.stream().sorted(Comparator.comparing(UUID::toString)).toList();
    }
}
