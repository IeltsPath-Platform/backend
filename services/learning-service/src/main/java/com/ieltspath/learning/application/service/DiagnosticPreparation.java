package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Loads the Content data a diagnosis may need before the caller takes the learner lock. */
@Component
public class DiagnosticPreparation {
    private final LearningContentClient content;
    private final KnowledgePointDirectory directory;
    private final PracticeAttemptRepository attempts;
    private final ReviewItemRepository reviews;

    public DiagnosticPreparation(LearningContentClient content, KnowledgePointDirectory directory,
                                 PracticeAttemptRepository attempts, ReviewItemRepository reviews) {
        this.content = content;
        this.directory = directory;
        this.attempts = attempts;
        this.reviews = reviews;
    }

    public DiagnosticFlow.Prepared forSources(UUID userId, List<UUID> sourceKnowledgePointIds) {
        if (sourceKnowledgePointIds == null || sourceKnowledgePointIds.isEmpty()) {
            return new DiagnosticFlow.Prepared(Map.of(), Map.of(), directory.describe());
        }
        Map<UUID, List<UUID>> fromContent = content.prerequisites(sourceKnowledgePointIds);
        Map<UUID, List<UUID>> prerequisites = new HashMap<>();
        if (fromContent != null) fromContent.forEach((key, value) ->
                prerequisites.put(key, value == null ? List.of() : List.copyOf(value)));
        Set<UUID> targets = new HashSet<>();
        prerequisites.values().stream().filter(java.util.Objects::nonNull).forEach(targets::addAll);
        Set<UUID> excluded = new HashSet<>(attempts.revealedPackageIds(userId));
        excluded.addAll(reviews.assignedPackageIds(userId));
        Map<UUID, List<LearningContentClient.PracticeSet>> candidates = new HashMap<>();
        for (UUID knowledgePointId : targets) {
            List<LearningContentClient.PracticeSet> found = content.searchPracticeSets(knowledgePointId,
                    new ArrayList<>(excluded), DiagnosticFlow.MIN_PROBE_QUESTIONS,
                    com.ieltspath.learning.domain.service.PrerequisiteDiagnosis.MAX_PROBES);
            candidates.put(knowledgePointId, found == null ? List.of() : List.copyOf(found));
        }
        return new DiagnosticFlow.Prepared(prerequisites, candidates, directory.describe());
    }

    public ReplacementData replacementsFor(UUID userId, UUID knowledgePointId) {
        Set<UUID> excluded = new HashSet<>(attempts.revealedPackageIds(userId));
        excluded.addAll(reviews.assignedPackageIds(userId));
        List<LearningContentClient.PracticeSet> found = content.searchPracticeSets(knowledgePointId,
                new ArrayList<>(excluded), DiagnosticFlow.MIN_PROBE_QUESTIONS,
                com.ieltspath.learning.domain.service.PrerequisiteDiagnosis.MAX_PROBES);
        List<PreparedSet> ready = new ArrayList<>();
        if (found != null) {
            for (LearningContentClient.PracticeSet set : found) {
                try {
                    ready.add(new PreparedSet(set, content.getPackageVersion(set.packageVersionId())));
                } catch (com.ieltspath.learning.application.exception.LearningRequestException error) {
                    if (error.getStatus() != 404 || !"NOT_FOUND".equals(error.getCode())) throw error;
                }
            }
        }
        return new ReplacementData(List.copyOf(ready), directory.describe());
    }

    public DiagnosticFlow.Prepared describeOnly() {
        return new DiagnosticFlow.Prepared(Map.of(), Map.of(), directory.describe());
    }

    public record PreparedSet(LearningContentClient.PracticeSet set, LearningContentClient.PackageVersion version) {}
    public record ReplacementData(List<PreparedSet> candidates,
                                 Map<UUID, KnowledgePointDirectory.Entry> directory) {}
}
