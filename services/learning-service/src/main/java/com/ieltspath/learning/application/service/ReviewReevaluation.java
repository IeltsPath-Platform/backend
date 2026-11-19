package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.entity.TopicProgress;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.repository.LessonProgressRepository;
import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.service.ReviewRule;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PendingReview;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/** Inserts reviews for weak, wrongly answered knowledge points after new evidence ({@link ReviewRule}). */
@Component
public class ReviewReevaluation {
    private final KnowledgeEvidenceRepository evidence;
    private final LearnerCurriculumRepository curricula;
    private final LessonProgressRepository lessons;
    private final ReviewItemRepository reviews;
    private final MasteryRulesProvider rules;
    private final MasteryProfileRepository profiles;

    public ReviewReevaluation(KnowledgeEvidenceRepository evidence, LearnerCurriculumRepository curricula,
                              LessonProgressRepository lessons, ReviewItemRepository reviews,
                              MasteryRulesProvider rules, MasteryProfileRepository profiles) {
        this.evidence = evidence;
        this.curricula = curricula;
        this.lessons = lessons;
        this.reviews = reviews;
        this.rules = rules;
        this.profiles = profiles;
    }

    /** Reviews from a graded test or Practice: blocking, and an optional review of the same point is upgraded. */
    public void execute(UUID userId, Set<UUID> consideredKps, Set<UUID> wrongKps) {
        run(userId, consideredKps, wrongKps, true, "ASSESSMENT");
    }

    private void run(UUID userId, Set<UUID> consideredKps, Set<UUID> wrongKps, boolean blocking,
                     String triggerKind) {
        // A review is created below the priority threshold, which Admin can change.
        ReviewRule rule = new ReviewRule(rules.current().priorityBelow());
        // The score is the one tiers and review schedules are built on; callers record evidence (and so the profile)
        // before asking for a re-evaluation. A point without a profile has no evidence and counts as 0.
        Map<UUID, Double> mastery = new HashMap<>();
        for (MasteryProfile profile : profiles.findAll(userId)) {
            mastery.put(profile.knowledgePointId(), profile.mastery());
        }
        Set<UUID> practiceKps = new HashSet<>();
        Map<UUID, LearningSkill> kpSkills = new HashMap<>();
        for (var history : evidence.findMasteryHistories(userId)) {
            if (history.hasPracticeSet()) practiceKps.add(history.knowledgePointId());
            if (history.skill() != null) kpSkills.put(history.knowledgePointId(), history.skill());
        }
        Map<UUID, Integer> topicOrder = new HashMap<>();
        Map<UUID, LearningSkill> topicSkills = new HashMap<>();
        for (TopicProgress topic : curricula.find(userId).topics()) {
            topicOrder.put(topic.topicId(), topic.sequenceOrder());
            topicSkills.put(topic.topicId(), topic.skill());
        }
        Map<UUID, List<ReviewRule.CompletedLesson>> lessonsByKp = new HashMap<>();
        for (var lesson : lessons.findCompleted(userId)) {
            var completed = new ReviewRule.CompletedLesson(lesson.lessonId(),
                    topicOrder.get(lesson.topicId()), lesson.sortOrder(), topicSkills.get(lesson.topicId()));
            for (UUID kpId : lesson.knowledgePointIds()) {
                lessonsByKp.computeIfAbsent(kpId, ignored -> new ArrayList<>()).add(completed);
            }
        }
        // A blocking review only waits for other blocking ones: an optional review of the same point is upgraded.
        Set<UUID> pending = reviews.findPending(userId).stream().filter(review -> !blocking || review.blocking())
                .map(PendingReview::knowledgePointId).collect(Collectors.toSet());
        var candidates = rule.reevaluate(consideredKps, wrongKps, mastery, lessonsByKp, practiceKps, pending,
                kpSkills);
        reviews.insertPending(userId, candidates, blocking, triggerKind);
        if (blocking) {
            reviews.promoteToBlocking(userId, candidates.stream().map(ReviewRule.ReviewCandidate::knowledgePointId)
                    .collect(Collectors.toSet()), triggerKind);
        }
    }
}
