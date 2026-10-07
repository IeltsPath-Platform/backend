package com.group01.learning.application.service;

import com.group01.learning.domain.entity.TopicProgress;
import com.group01.learning.domain.repository.KnowledgeEvidenceRepository;
import com.group01.learning.domain.repository.LearnerCurriculumRepository;
import com.group01.learning.domain.repository.LessonProgressRepository;
import com.group01.learning.domain.repository.ReviewItemRepository;
import com.group01.learning.domain.service.MasteryCalculator;
import com.group01.learning.domain.service.ReviewRule;
import com.group01.learning.domain.vo.LearningSkill;
import com.group01.learning.domain.vo.PendingReview;
import org.springframework.beans.factory.annotation.Value;
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
    private final ReviewRule rule;
    private final MasteryCalculator masteryCalculator = new MasteryCalculator();

    public ReviewReevaluation(KnowledgeEvidenceRepository evidence, LearnerCurriculumRepository curricula,
                              LessonProgressRepository lessons, ReviewItemRepository reviews,
                              @Value("${learning.review-mastery-threshold:0.6}") double threshold) {
        this.evidence = evidence;
        this.curricula = curricula;
        this.lessons = lessons;
        this.reviews = reviews;
        this.rule = new ReviewRule(threshold);
    }

    public void execute(UUID userId, Set<UUID> consideredKps, Set<UUID> wrongKps) {
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
        Set<UUID> pending = reviews.findPending(userId).stream().map(PendingReview::knowledgePointId)
                .collect(Collectors.toSet());
        reviews.insertPending(userId, rule.reevaluate(consideredKps, wrongKps, mastery, lessonsByKp, practiceKps,
                pending, kpSkills));
    }
}
