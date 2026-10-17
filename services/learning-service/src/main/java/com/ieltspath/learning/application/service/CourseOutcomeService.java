package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.CourseScoreData;
import com.ieltspath.learning.domain.aggregate.CourseProgress;
import com.ieltspath.learning.domain.repository.CourseOutcomeRepository;
import com.ieltspath.learning.domain.repository.CourseProgressRepository;
import com.ieltspath.learning.domain.repository.CourseScoreConfigRepository;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.repository.RemediationFlagRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.vo.CourseOutcome;
import com.ieltspath.learning.domain.vo.CourseScoreConfig;
import com.ieltspath.learning.domain.vo.PendingReview;
import com.ieltspath.learning.domain.vo.RemediationFlag;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * What a finished topic or course test means for the learner's course: the mid-course remediation flag after a weak
 * first topic test, and the decision (pass, or remediation then retest) after a course test. It runs inside the
 * transaction that applied the result, after the blocking reviews of the wrong knowledge points exist.
 */
@Service
public class CourseOutcomeService {
    private static final int TOP_ERRORS = 5;

    private final CourseScoreConfigRepository configs;
    private final CourseScoreService scores;
    private final CourseScoreData data;
    private final CourseOutcomeRepository outcomes;
    private final RemediationFlagRepository flags;
    private final ReviewItemRepository reviews;
    private final LearnerCurriculumRepository curricula;
    private final CourseProgressRepository courseProgress;
    private final BadgeService badges;
    private final Clock clock = Clock.systemUTC();

    public CourseOutcomeService(CourseScoreConfigRepository configs, CourseScoreService scores, CourseScoreData data,
                                CourseOutcomeRepository outcomes, RemediationFlagRepository flags,
                                ReviewItemRepository reviews, LearnerCurriculumRepository curricula,
                                CourseProgressRepository courseProgress, BadgeService badges) {
        this.badges = badges;
        this.configs = configs;
        this.scores = scores;
        this.data = data;
        this.outcomes = outcomes;
        this.flags = flags;
        this.reviews = reviews;
        this.curricula = curricula;
        this.courseProgress = courseProgress;
    }

    /** The first test of a topic below the configured line asks for supplementary work; later tests do not. */
    public void afterTopicTest(UUID userId, UUID topicId, UUID attemptId, double percent, boolean firstTest,
                               Set<UUID> wrongKnowledgePoints) {
        // Called once per recorded topic test, so any topic test the learner takes earns the first-test badge.
        badges.onTopicTest(userId);
        if (!firstTest) return;
        UUID courseId = curricula.find(userId).topic(topicId).map(topic -> topic.courseId()).orElse(null);
        if (courseId == null) return;
        CourseScoreConfig config = configs.effective(courseId);
        if (BigDecimal.valueOf(percent).compareTo(config.midRemediationBelow().movePointRight(2)) >= 0) return;
        flags.insert(new RemediationFlag(UUID.randomUUID(), userId, courseId, topicId,
                RemediationFlag.Kind.MID_COURSE, attemptId, blockingReviewsOf(userId, wrongKnowledgePoints),
                clock.instant(), true));
    }

    /**
     * Decides the course from the total score once a course test is consumed. At or above the threshold the course is
     * passed; below it the learner gets remediation, and may retest when its reviews are done.
     */
    public void afterCourseTest(UUID userId, UUID courseId, UUID attemptId, Set<UUID> wrongKnowledgePoints) {
        CourseScoreConfig config = configs.effective(courseId);
        var evaluation = scores.evaluate(userId, courseId, config);
        double total = evaluation.score().total();
        var components = CourseScoreService.asMap(evaluation);
        if (scores.passes(config, total)) {
            outcomes.save(new CourseOutcome(userId, courseId, CourseOutcome.Status.PASSED, total, components,
                    attemptId, List.of(), List.of(), clock.instant(), false));
            CourseProgress progress = courseProgress.findAll(userId).get(courseId);
            if (progress == null) progress = CourseProgress.restore(userId, courseId, null);
            if (progress.pass(clock.instant())) courseProgress.save(progress);
            badges.onCoursePassed(userId);
            return;
        }
        List<UUID> reviewIds = blockingReviewsOf(userId, wrongKnowledgePoints);
        List<String> errors = data.topErrors(userId, attemptId, TOP_ERRORS).stream()
                .map(CourseScoreData.ErrorCount::code).toList();
        outcomes.save(new CourseOutcome(userId, courseId, CourseOutcome.Status.REMEDIATION, total, components,
                attemptId, errors, reviewIds, clock.instant(), !reviewIds.isEmpty()));
        flags.insert(new RemediationFlag(UUID.randomUUID(), userId, courseId, null, RemediationFlag.Kind.POST_COURSE,
                attemptId, reviewIds, clock.instant(), true));
    }

    /** The learner's pending blocking reviews of the given knowledge points, oldest first. */
    private List<UUID> blockingReviewsOf(UUID userId, Set<UUID> knowledgePoints) {
        return reviews.findPending(userId).stream().filter(PendingReview::blocking)
                .filter(review -> knowledgePoints.contains(review.knowledgePointId()))
                .map(PendingReview::reviewId).toList();
    }
}
