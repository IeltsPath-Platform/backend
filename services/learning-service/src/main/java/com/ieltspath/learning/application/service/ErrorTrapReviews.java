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

    /**
     * Reviews for the error codes an examiner marked, most marked first. Unlike {@link #create}, a single marking is enough: the
     * examiner has seen the error, so the parent point is reviewed now rather than when its trap point reaches PRIORITY. A point
     * with a pending review is skipped; the learner is sent to a lesson that teaches the point. Returns how many were created.
     */
    public int createForMarkedCodes(UUID userId, UUID submissionLessonId, Collection<MarkedErrorTrap> marked, int maxReviews) {
        if (maxReviews <= 0 || marked == null || marked.isEmpty()) return 0;
        ErrorTrapCatalog.ErrorTraps traps = catalog.load();
        if (traps == null || traps.parentByTrapKp().isEmpty()) return 0;

        Set<UUID> pendingKps = reviews.findPending(userId).stream()
                .map(PendingReview::knowledgePointId).collect(Collectors.toSet());
        Set<UUID> excluded = new HashSet<>(attempts.revealedPackageIds(userId));
        excluded.addAll(reviews.assignedPackageIds(userId));
        List<UUID> excludedIds = excluded.stream().sorted(Comparator.comparing(UUID::toString)).toList();
        int created = 0;

        for (MarkedErrorTrap mark : marked) {
            if (created >= maxReviews) break;
            UUID parent = traps.parentByTrapKp().get(mark.trapKnowledgePointId());
            if (parent == null || pendingKps.contains(parent)) continue;

            UUID lessonId = teachingSubmissionLesson(userId, submissionLessonId, parent);
            if (lessonId == null) continue;
            List<LearningContentClient.PracticeSet> candidates;
            try {
                candidates = content.searchPracticeSets(parent, excludedIds, MIN_SET_QUESTIONS, 1, lessonId, mark.code());
            } catch (RuntimeException exception) {
                log.warn("Skipping marked error review: userId={}, errorCode={}, reason={}",
                        userId, mark.code(), exception.getMessage());
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
        return created;
    }

    /** A trap point an examiner's marking counts against, with how often the examiner marked its error. */
    public record MarkedErrorTrap(String code, UUID trapKnowledgePointId, int count) { }

    /** One marked trap read ahead of the lock: its parent point, the lesson to send the learner to, and whether it has sets. */
    public record PlannedTrap(UUID trapKnowledgePointId, String code, UUID parent, UUID lessonId, boolean hasTrapSets) { }

    /**
     * Reads what the reviews of a marking need from Content, before the learner's lock and transaction. Creating them afterwards,
     * with {@link #createPlanned}, makes no Content call.
     */
    public List<PlannedTrap> plan(UUID userId, UUID submissionLessonId, Collection<MarkedErrorTrap> marked) {
        if (marked == null || marked.isEmpty()) return List.of();
        ErrorTrapCatalog.ErrorTraps traps = catalog.load();
        if (traps == null || traps.parentByTrapKp().isEmpty()) return List.of();
        Set<UUID> excluded = new HashSet<>(attempts.revealedPackageIds(userId));
        excluded.addAll(reviews.assignedPackageIds(userId));
        List<UUID> excludedIds = excluded.stream().sorted(Comparator.comparing(UUID::toString)).toList();
        List<PlannedTrap> planned = new ArrayList<>();
        for (MarkedErrorTrap mark : marked) {
            UUID parent = traps.parentByTrapKp().get(mark.trapKnowledgePointId());
            if (parent == null) continue;
            UUID lessonId = teachingSubmissionLesson(userId, submissionLessonId, parent);
            boolean hasTrapSets = false;
            if (lessonId != null) {
                try {
                    List<LearningContentClient.PracticeSet> candidates = content.searchPracticeSets(parent, excludedIds,
                            MIN_SET_QUESTIONS, 1, lessonId, mark.code());
                    hasTrapSets = candidates != null
                            && candidates.stream().anyMatch(set -> set.trapQuestionCount() >= MIN_TRAP_QUESTIONS);
                } catch (RuntimeException exception) {
                    log.warn("Skipping marked error review: userId={}, errorCode={}, reason={}",
                            userId, mark.code(), exception.getMessage());
                }
            }
            planned.add(new PlannedTrap(mark.trapKnowledgePointId(), mark.code(), parent, lessonId, hasTrapSets));
        }
        return planned;
    }

    /** Creates the planned reviews under the learner's lock, from the planned data alone. Returns how many were created. */
    public int createPlanned(UUID userId, Collection<PlannedTrap> planned, int maxReviews) {
        if (maxReviews <= 0 || planned.isEmpty()) return 0;
        Set<UUID> pendingKps = reviews.findPending(userId).stream()
                .map(PendingReview::knowledgePointId).collect(Collectors.toSet());
        int created = 0;
        for (PlannedTrap trap : planned) {
            if (created >= maxReviews) break;
            if (!trap.hasTrapSets() || trap.lessonId() == null || pendingKps.contains(trap.parent())) continue;
            LearningSkill skill = evidence.findMasteryHistories(userId, List.of(trap.parent())).stream()
                    .map(history -> history.skill()).filter(java.util.Objects::nonNull).findFirst().orElse(null);
            reviews.insertPending(userId, List.of(new ReviewCandidate(trap.parent(), trap.lessonId(), skill)), false, "ERROR_PATTERN");
            pendingKps.add(trap.parent());
            created++;
        }
        return created;
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
