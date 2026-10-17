package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.entity.TopicProgress;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.repository.LessonProgressRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.service.MasteryCalculator;
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
    private final MasteryCalculator masteryCalculator = new MasteryCalculator();

    public ReviewReevaluation(KnowledgeEvidenceRepository evidence, LearnerCurriculumRepository curricula,
                              LessonProgressRepository lessons, ReviewItemRepository reviews,
                              MasteryRulesProvider rules) {
        this.evidence = evidence;
        this.curricula = curricula;
        this.lessons = lessons;
        this.reviews = reviews;
        this.rules = rules;
    }

    /** Reviews from a graded test or Practice: blocking, and an optional review of the same point is upgraded. */
    public void execute(UUID userId, Set<UUID> consideredKps, Set<UUID> wrongKps) {
        run(userId, consideredKps, wrongKps, true, "ASSESSMENT");
    }

    /** Reviews raised by a test that does not count (a free practice test): optional, they never block. */
    public void executeOptional(UUID userId, Set<UUID> consideredKps, Set<UUID> wrongKps, String triggerKind) {
        run(userId, consideredKps, wrongKps, false, triggerKind);
    }

    private void run(UUID userId, Set<UUID> consideredKps, Set<UUID> wrongKps, boolean blocking,
                     String triggerKind) {
        // A review is created below the priority threshold, which Admin can change.
        ReviewRule rule = new ReviewRule(rules.current().priorityBelow());
        Map<UUID, Double> mastery = new HashMap<>();
        Set<UUID> practiceKps = new HashSet<>();
        Map<UUID, LearningSkill> kpSkills = new HashMap<>();
        for (var history : evidence.findMasteryHistories(userId)) {
            mastery.put(history.knowledgePointId(), masteryCalculator.compute(history.correctness()));
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
