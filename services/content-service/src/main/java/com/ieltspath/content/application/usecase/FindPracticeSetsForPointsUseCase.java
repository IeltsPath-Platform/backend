package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.CourseCandidateReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The published practice sets that belong to no lesson and measure at least one of the given knowledge points, which is
 * what the course generator may offer for a lesson. An author choosing sets by hand sees the same ones marked.
 */
@Service
@Transactional(readOnly = true)
public class FindPracticeSetsForPointsUseCase {

    static final int MAX_POINTS = 100;

    private final CourseCandidateReader candidates;

    public FindPracticeSetsForPointsUseCase(CourseCandidateReader candidates) {
        this.candidates = candidates;
    }

    public Set<UUID> execute(Collection<UUID> pointIds) {
        if (pointIds.isEmpty()) return Set.of();
        if (pointIds.size() > MAX_POINTS) {
            throw new IllegalArgumentException("knowledgePointIds must contain at most " + MAX_POINTS + " ids");
        }
        Set<UUID> packageIds = new LinkedHashSet<>();
        candidates.practiceSets(pointIds, Integer.MAX_VALUE).values()
                .forEach(sets -> sets.forEach(set -> packageIds.add(set.packageId())));
        return packageIds;
    }
}
