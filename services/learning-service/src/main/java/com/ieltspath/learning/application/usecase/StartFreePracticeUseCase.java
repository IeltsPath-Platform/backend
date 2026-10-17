package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.PackageVersion;
import com.ieltspath.learning.application.port.LearningContentClient.PracticeSet;
import com.ieltspath.learning.application.port.LearningContentClient.Topic;
import com.ieltspath.learning.application.result.PracticeAttemptView;
import com.ieltspath.learning.application.service.HeartsService;
import com.ieltspath.learning.application.service.ItemGrading;
import com.ieltspath.learning.application.service.KnowledgePointDirectory;
import com.ieltspath.learning.application.service.PracticeAttemptViewAssembler;
import com.ieltspath.learning.domain.aggregate.PracticeAttempt;
import com.ieltspath.learning.domain.exception.LearningGateException;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PendingReview;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Starts free practice on one knowledge point: a set the learner has not seen, in a course the learner is taking. The
 * last {@code learning.free-practice.reserve} unseen sets of the point stay for review, so free practice never uses
 * up what a review needs.
 */
@Service
public class StartFreePracticeUseCase {
    /** Sets fetched from Content before one within band, plan and with no essay is chosen; 10 is Content's maximum. */
    private static final int CANDIDATES = 10;

    private final LearnerLock lock;
    private final LearningContentClient content;
    private final KnowledgePointDirectory directory;
    private final LearnerCurriculumRepository curricula;
    private final ReviewItemRepository reviews;
    private final PracticeAttemptRepository attempts;
    private final PracticeAttemptViewAssembler view;
    private final int reserve;
    private final HeartsService hearts;
    private final AccessClient access;
    private final Clock clock = Clock.systemUTC();

    public StartFreePracticeUseCase(LearnerLock lock, LearningContentClient content, KnowledgePointDirectory directory,
                                    LearnerCurriculumRepository curricula, ReviewItemRepository reviews,
                                    PracticeAttemptRepository attempts, PracticeAttemptViewAssembler view,
                                    HeartsService hearts, AccessClient access,
                                    @Value("${learning.free-practice.reserve:2}") int reserve) {
        this.access = access;
        this.hearts = hearts;
        this.lock = lock;
        this.content = content;
        this.directory = directory;
        this.curricula = curricula;
        this.reviews = reviews;
        this.attempts = attempts;
        this.view = view;
        this.reserve = reserve;
    }

    @Transactional
    public PracticeAttemptView execute(UUID userId, UUID knowledgePointId) {
        lock.lock(userId);
        hearts.requireHearts(userId);
        Topic topic = topicOf(knowledgePointId);
        requireOpenCourse(userId, topic);
        List<PendingReview> blocking = reviews.findPending(userId).stream().filter(PendingReview::blocking)
                .filter(review -> review.knowledgePointId().equals(knowledgePointId)).toList();
        if (!blocking.isEmpty()) throw new LearningGateException("REVIEW_REQUIRED", blocking);

        Set<UUID> excluded = new HashSet<>(attempts.revealedPackageIds(userId));
        excluded.addAll(reviews.assignedPackageIds(userId));
        List<UUID> excludedList = excluded.stream().sorted(Comparator.comparing(UUID::toString)).toList();
        int unseen = content.practiceSetAvailability(List.of(knowledgePointId), excludedList,
                GetReviewBasketUseCase.MIN_SET_QUESTIONS).getOrDefault(knowledgePointId, 0);
        if (unseen <= reserve) throw exhausted();

        LearningSkill skill = skillOf(topic, knowledgePointId).orElseThrow(() -> new LearningRequestException(409,
                "FREE_PRACTICE_UNAVAILABLE", "This knowledge point has no skill to practise"));
        List<PracticeSet> candidates = content.searchPracticeSets(knowledgePointId, excludedList,
                GetReviewBasketUseCase.MIN_SET_QUESTIONS, CANDIDATES);
        // One question to Access for the whole list, and only when some set needs a plan.
        boolean premium = candidates.stream().anyMatch(set -> set.requiredFeatureKey() != null) && hasPlan();
        BigDecimal courseBand = topic.course() == null ? null : topic.course().bandLevel();
        for (PracticeSet candidate : candidates) {
            if (!withinBand(candidate, courseBand)) continue;
            if (candidate.requiredFeatureKey() != null && !premium) continue;
            PackageVersion version = content.getPackageVersion(candidate.packageVersionId());
            if (ItemGrading.orderedItems(version).stream().anyMatch(ItemGrading::isEssay)) continue;
            Optional<PracticeAttempt> open = attempts.findOpen(userId, candidate.packageId());
            if (open.isPresent()) return view.assemble(open.get(), version);
            PracticeAttempt attempt = PracticeAttempt.startFree(UUID.randomUUID(), userId, knowledgePointId,
                    Set.of(skill), candidate.packageId(), candidate.packageVersionId(), clock.instant());
            attempts.insert(attempt);
            return view.assemble(attempt, version);
        }
        throw exhausted();
    }

    /** A set above the band of the learner's course is too hard to offer; a set with no band has no limit. */
    private static boolean withinBand(PracticeSet set, BigDecimal courseBand) {
        return set.band() == null || courseBand == null || set.band().compareTo(courseBand) <= 0;
    }

    /**
     * Whether the learner has an active plan. A set that needs a feature is offered only to a learner with one; when Access
     * cannot answer, the learner is treated as having none.
     */
    private boolean hasPlan() {
        try {
            return access.premium();
        } catch (AccessUnavailableException unavailable) {
            return false;
        }
    }

    /** The topic that teaches the point, which Content returns only for active points. */
    private Topic topicOf(UUID knowledgePointId) {
        return content.getTopicSequence().stream()
                .filter(topic -> topic.knowledgePoints().stream().anyMatch(kp -> kp.id().equals(knowledgePointId)))
                .findFirst()
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Knowledge point was not found"));
    }

    /** The learner must be taking the course of the point, and the course must be open. */
    private void requireOpenCourse(UUID userId, Topic topic) {
        if (topic.course() == null || !topic.course().active()) {
            throw new LearningRequestException(403, "COURSE_NOT_OPEN", "The course of this knowledge point is closed");
        }
        boolean taking = curricula.find(userId).topics().stream()
                .anyMatch(progress -> progress.topicId().equals(topic.topicId()));
        if (!taking) {
            throw new LearningRequestException(403, "COURSE_NOT_JOINED", "The learner is not taking this course");
        }
    }

    /** The point's skill as Content names it; a point whose skill is missing or unknown cannot be practised freely. */
    private Optional<LearningSkill> skillOf(Topic topic, UUID knowledgePointId) {
        String named = directory.describe().getOrDefault(knowledgePointId, KnowledgePointDirectory.Entry.UNKNOWN).skill();
        if (named == null) named = topic.skill() == null ? null : topic.skill().name();
        String skillName = named;
        return Arrays.stream(LearningSkill.values()).filter(skill -> Objects.equals(skill.name(), skillName))
                .findFirst();
    }

    private static LearningRequestException exhausted() {
        return new LearningRequestException(409, "FREE_PRACTICE_EXHAUSTED",
                "No practice set is left for this knowledge point; the rest is kept for reviews");
    }
}
