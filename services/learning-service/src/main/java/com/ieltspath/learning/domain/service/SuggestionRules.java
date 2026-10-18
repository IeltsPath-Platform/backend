package com.ieltspath.learning.domain.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Picks the optional suggestions shown to a learner. Targets come in priority order (weak points to consolidate or
 * due for review first, then the next points in the tree); each target gets at most one suggestion, the first unseen
 * candidate of its preferred type that is not above the learner's band. A suggestion never blocks anything.
 */
public final class SuggestionRules {
    public static final int MAX_SUGGESTIONS = 5;

    public enum Type { PRACTICE_SET, PRACTICE_TEST, LESSON }

    public enum Reason { CONSOLIDATE, DUE, NEXT_IN_TREE }

    public record Target(UUID knowledgePointId, Reason reason) {}

    /** {@code id} is the package id or the lesson id; {@code seen} marks content the learner has already done. */
    public record Candidate(Type type, UUID id, UUID versionId, String title, List<UUID> knowledgePointIds,
                            BigDecimal band, String skill, boolean seen) {}

    public record Suggestion(Type type, Candidate candidate, UUID knowledgePointId, Reason reason) {}

    public List<Suggestion> choose(List<Target> targets, List<Candidate> candidates, BigDecimal maxBand) {
        List<Suggestion> chosen = new ArrayList<>();
        Set<String> taken = new HashSet<>();
        for (Target target : targets) {
            if (chosen.size() >= MAX_SUGGESTIONS) break;
            for (Type type : preference(target.reason())) {
                var match = candidates.stream()
                        .filter(candidate -> candidate.type() == type && !candidate.seen()
                                && candidate.knowledgePointIds().contains(target.knowledgePointId())
                                && (candidate.band() == null || candidate.band().compareTo(maxBand) <= 0)
                                && !taken.contains(key(candidate)))
                        .findFirst();
                if (match.isPresent()) {
                    taken.add(key(match.get()));
                    chosen.add(new Suggestion(type, match.get(), target.knowledgePointId(), target.reason()));
                    break;
                }
            }
        }
        return List.copyOf(chosen);
    }

    private static List<Type> preference(Reason reason) {
        return reason == Reason.NEXT_IN_TREE ? List.of(Type.LESSON, Type.PRACTICE_SET)
                : List.of(Type.PRACTICE_SET, Type.PRACTICE_TEST, Type.LESSON);
    }

    private static String key(Candidate candidate) {
        return candidate.type() + ":" + candidate.id();
    }
}
