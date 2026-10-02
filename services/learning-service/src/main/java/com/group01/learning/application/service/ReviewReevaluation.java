package com.group01.learning.application;

import com.group01.learning.application.port.LearningProgressStore;
import com.group01.learning.domain.service.MasteryCalculator;
import com.group01.learning.domain.service.ReviewRule;
import com.group01.learning.domain.vo.TopicProgress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ReviewReevaluation {
    private final LearningProgressStore store;
    private final ReviewRule rule;
    private final MasteryCalculator masteryCalculator = new MasteryCalculator();

    public ReviewReevaluation(LearningProgressStore store,
                              @Value("${learning.review-mastery-threshold:0.6}") double threshold) {
        this.store = store;
        this.rule = new ReviewRule(threshold);
    }

    public void execute(UUID userId, Set<UUID> consideredKps, Set<UUID> wrongKps) {
        var histories = store.findMastery(userId);
        Map<UUID, Double> mastery = new HashMap<>();
        Set<UUID> practiceKps = new HashSet<>();
        for (var history : histories) {
            mastery.put(history.knowledgePointId(), masteryCalculator.compute(history.correctness()));
            if (history.hasPracticeSet()) practiceKps.add(history.knowledgePointId());
        }
        Map<UUID, Integer> topicOrder = new HashMap<>();
        for (TopicProgress topic : store.findTopics(userId)) topicOrder.put(topic.topicId(), topic.sequenceOrder());
        Map<UUID, List<ReviewRule.CompletedLesson>> lessonsByKp = new HashMap<>();
        for (var lesson : store.findCompletedLessons(userId)) {
            var completed = new ReviewRule.CompletedLesson(lesson.lessonId(),
                    topicOrder.get(lesson.topicId()), lesson.sortOrder());
            for (UUID kpId : lesson.knowledgePointIds()) {
                lessonsByKp.computeIfAbsent(kpId, ignored -> new ArrayList<>()).add(completed);
            }
        }
        Set<UUID> pending = store.findPendingReviews(userId).stream()
                .map(review -> review.knowledgePointId()).collect(Collectors.toSet());
        store.insertReviews(userId, rule.reevaluate(consideredKps, wrongKps, mastery,
                lessonsByKp, practiceKps, pending));
    }
}
