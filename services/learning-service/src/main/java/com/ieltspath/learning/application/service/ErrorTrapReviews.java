package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.service.MainErrorRule;
import com.ieltspath.learning.domain.service.ReviewRule.ReviewCandidate;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.MasteryTier;
import com.ieltspath.learning.domain.vo.PendingReview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Creates optional parent-point reviews after evidence has refreshed the child's mastery. */
@Service
public class ErrorTrapReviews {
    private static final Logger log = LoggerFactory.getLogger(ErrorTrapReviews.class);
    private static final int MIN_SET_QUESTIONS = 3;
    private static final int MIN_TRAP_QUESTIONS = 2;

    private final ReviewItemRepository reviews;
    private final MasteryProfileRepository profiles;
    private final PracticeAttemptRepository attempts;
    private final KnowledgeEvidenceRepository evidence;
    private final LearningContentClient content;
    private final CompletedLessonLocator lessonLocator;
    private final ErrorTrapCatalog catalog;
    private final int limit;

    public ErrorTrapReviews(ReviewItemRepository reviews, MasteryProfileRepository profiles,
                            PracticeAttemptRepository attempts, KnowledgeEvidenceRepository evidence,
                            LearningContentClient content, CompletedLessonLocator lessonLocator,
                            ErrorTrapCatalog catalog,
                            @Value("${learning.error-trap.review-limit:1}") int limit) {
        if (limit < 0) throw new IllegalArgumentException("error-trap.review-limit must not be negative");
        this.reviews = reviews;
        this.profiles = profiles;
        this.attempts = attempts;
        this.evidence = evidence;
        this.content = content;
        this.lessonLocator = lessonLocator;
        this.catalog = catalog;
        this.limit = limit;
    }

    public void create(UUID userId, UUID submissionLessonId, Collection<UUID> trapKpsWithNewEvidence) {
        if (limit == 0 || trapKpsWithNewEvidence == null || trapKpsWithNewEvidence.isEmpty()) return;
        ErrorTrapCatalog.ErrorTraps traps = catalog.load();
        if (traps == null || traps.parentByTrapKp().isEmpty()) return;

        List<MasteryProfile> trapProfiles = profiles.findErrorTrapProfiles(userId);
        Map<UUID, MasteryProfile> byTrap = trapProfiles.stream().collect(Collectors.toMap(
                MasteryProfile::knowledgePointId, Function.identity(), (first, ignored) -> first));
        Map<UUID, String> codeByTrap = traps.trapKpByCode().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        List<UUID> ordered = trapKpsWithNewEvidence.stream().distinct()
                .filter(byTrap::containsKey)
                .sorted(Comparator.comparingDouble((UUID kp) -> byTrap.get(kp).mastery())
                        .thenComparing(kp -> codeByTrap.getOrDefault(kp, "")))
                .toList();
        if (ordered.isEmpty()) return;

        Set<UUID> pendingKps = reviews.findPending(userId).stream()
                .map(PendingReview::knowledgePointId).collect(Collectors.toSet());
        Set<UUID> excluded = new HashSet<>(attempts.revealedPackageIds(userId));
        excluded.addAll(reviews.assignedPackageIds(userId));
        List<UUID> excludedIds = excluded.stream().sorted(Comparator.comparing(UUID::toString)).toList();
        int created = 0;

        for (UUID trapKp : ordered) {
            if (created >= limit) break;
            MasteryProfile profile = byTrap.get(trapKp);
            if (profile.tier() != MasteryTier.PRIORITY) continue;
            UUID parent = traps.parentByTrapKp().get(trapKp);
            if (parent == null || pendingKps.contains(parent)) continue;
            String errorCode = codeByTrap.get(trapKp);
            if (errorCode == null) continue;

            UUID lessonId = teachingSubmissionLesson(userId, submissionLessonId, parent);
            if (lessonId == null) continue;
            List<LearningContentClient.PracticeSet> candidates;
            try {
                candidates = content.searchPracticeSets(parent, excludedIds, MIN_SET_QUESTIONS, 1, lessonId, errorCode);
            } catch (RuntimeException exception) {
                log.warn("Skipping error-trap review creation: userId={}, trapKpId={}, errorCode={}, reason={}",
                        userId, trapKp, errorCode, exception.getMessage());
                continue;
            }
            if (candidates == null || candidates.stream().noneMatch(set -> set.trapQuestionCount() >= MIN_TRAP_QUESTIONS)) {
                continue;
            }

            LearningSkill skill = evidence.findMasteryHistories(userId, List.of(parent)).stream()
                    .map(history -> history.skill()).filter(java.util.Objects::nonNull).findFirst().orElse(null);
            reviews.insertPending(userId, List.of(new ReviewCandidate(parent, lessonId, skill)), false, "ERROR_PATTERN");
            pendingKps.add(parent);
            created++;
        }
    }

    private UUID teachingSubmissionLesson(UUID userId, UUID submissionLessonId, UUID parent) {
        if (submissionLessonId != null) {
            try {
                LearningContentClient.Lesson lesson = content.getLesson(submissionLessonId);
                if (lesson != null && lesson.knowledgePointIds() != null && lesson.knowledgePointIds().contains(parent)) {
                    return submissionLessonId;
                }
            } catch (RuntimeException exception) {
                log.debug("Unable to inspect submission lesson for error-trap review: lessonId={}", submissionLessonId,
                        exception);
            }
        }
        return lessonLocator.teaching(userId, parent).orElse(null);
    }
}
