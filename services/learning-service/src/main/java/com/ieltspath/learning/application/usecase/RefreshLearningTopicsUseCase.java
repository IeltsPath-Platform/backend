package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.TopicResult;
import com.ieltspath.learning.domain.aggregate.LearnerCurriculum.TopicPlacement;
import com.ieltspath.learning.domain.aggregate.LearnerCurriculum;
import com.ieltspath.learning.domain.repository.KnowledgePointCatalogRepository;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.repository.LessonProgressRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.vo.KnowledgePointCatalogEntry;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Takes the topic order and knowledge point catalog from Content and returns the learner's topics with status. */
@Service
public class RefreshLearningTopicsUseCase {
    private final LearningContentClient content;
    private final LearnerLock lock;
    private final LearnerCurriculumRepository curricula;
    private final KnowledgePointCatalogRepository catalog;
    private final LessonProgressRepository lessons;
    private final ReviewItemRepository reviews;

    public RefreshLearningTopicsUseCase(LearningContentClient content, LearnerLock lock,
                                        LearnerCurriculumRepository curricula, KnowledgePointCatalogRepository catalog,
                                        LessonProgressRepository lessons, ReviewItemRepository reviews) {
        this.content = content;
        this.lock = lock;
        this.curricula = curricula;
        this.catalog = catalog;
        this.lessons = lessons;
        this.reviews = reviews;
    }

    @Transactional
    public List<TopicResult> execute(UUID userId) {
        lock.lock(userId);
        var topics = content.getTopicSequence()
                .stream()
                .sorted(Comparator.comparing((LearningContentClient.Topic topic) ->
                                topic.course() == null ? null : topic.course().bandLevel(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(topic -> topic.requiredFeatureKey() == null ? 0 : 1)
                        .thenComparingInt(LearningContentClient.Topic::sortOrder)
                        .thenComparing(topic -> topic.topicId().toString()))
                .toList();
        List<KnowledgePointCatalogEntry> entries = new ArrayList<>();
        for (var topic : topics) {
            for (var kp : topic.knowledgePoints()) {
                LearningSkill skill = kp.skill() == null ? topic.skill() : LearningSkill.valueOf(kp.skill());
                entries.add(new KnowledgePointCatalogEntry(kp.id(), topic.topicId(), kp.hasPracticeSet(), skill));
            }
        }
        LearnerCurriculum curriculum = curricula.find(userId);
        curriculum.reorder(topics.stream().map(topic -> new TopicPlacement(
                topic.topicId(), topic.course() == null ? null : topic.course().courseId(),
                topic.skills(), topic.hasTopicTest())).toList());
        curricula.save(curriculum);
        catalog.upsert(entries);
        reviews.backfillMissingSkill(userId);
        var statuses = curriculum.statuses();
        var counts = lessons.completedCountsByTopic(userId);
        List<TopicResult> results = new ArrayList<>();
        for (int index = 0; index < topics.size(); index++) {
            var topic = topics.get(index);
            results.add(new TopicResult(topic.topicId(), topic.code(), topic.name(), index + 1,
                    statuses.get(topic.topicId()), counts.getOrDefault(topic.topicId(), 0),
                    topic.requiredFeatureKey(), topic.skill(), topic.hasTopicTest(), topic.course() == null ? null
                            : new TopicResult.Course(topic.course().courseId(), topic.course().code(),
                            topic.course().name(), topic.course().bandLevel()),
                    topic.course() != null && topic.course().hasCourseTest(), topic.skills()));
        }
        return List.copyOf(results);
    }
}
