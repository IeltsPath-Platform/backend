package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.UnlinkedPracticeSetReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The published practice sets that belong to no lesson and measure at least one of the given knowledge points, which is
 * what an author may attach to a lesson. The picker marks these.
 */
@Service
@Transactional(readOnly = true)
public class FindPracticeSetsForPointsUseCase {

    static final int MAX_POINTS = 100;

    private final UnlinkedPracticeSetReader practiceSets;

    public FindPracticeSetsForPointsUseCase(UnlinkedPracticeSetReader practiceSets) {
        this.practiceSets = practiceSets;
    }

    public Set<UUID> execute(Collection<UUID> pointIds) {
        if (pointIds.isEmpty()) return Set.of();
        if (pointIds.size() > MAX_POINTS) {
            throw new IllegalArgumentException("knowledgePointIds must contain at most " + MAX_POINTS + " ids");
        }
        Set<UUID> packageIds = new LinkedHashSet<>();
        practiceSets.practiceSets(pointIds, Integer.MAX_VALUE).values()
                .forEach(sets -> sets.forEach(set -> packageIds.add(set.packageId())));
        return packageIds;
    }
}
