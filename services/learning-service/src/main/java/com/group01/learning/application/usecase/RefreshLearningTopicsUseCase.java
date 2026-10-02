package com.group01.learning.application.usecase;

import com.group01.learning.application.port.LearnerLock;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.result.TopicResult;
import com.group01.learning.domain.aggregate.LearnerCurriculum;
import com.group01.learning.domain.repository.KnowledgePointCatalogRepository;
import com.group01.learning.domain.repository.LearnerCurriculumRepository;
import com.group01.learning.domain.repository.LessonProgressRepository;
import com.group01.learning.domain.vo.KnowledgePointCatalogEntry;
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

    public RefreshLearningTopicsUseCase(LearningContentClient content, LearnerLock lock,
                                        LearnerCurriculumRepository curricula, KnowledgePointCatalogRepository catalog,
                                        LessonProgressRepository lessons) {
        this.content = content;
        this.lock = lock;
        this.curricula = curricula;
        this.catalog = catalog;
        this.lessons = lessons;
    }

    @Transactional
    public List<TopicResult> execute(UUID userId) {
        lock.lock(userId);
        var topics = content.getTopicSequence()
                .stream()
                .sorted(Comparator
                .comparingInt(LearningContentClient.Topic::sortOrder)
                .thenComparing(
                        topic -> topic.topicId().toString()))
                .toList();
        List<KnowledgePointCatalogEntry> entries = new ArrayList<>();
        for (var topic : topics) {
            for (var kp : topic.knowledgePoints()) {
                entries.add(new KnowledgePointCatalogEntry(kp.id(), topic.topicId(), kp.hasPracticeSet()));
            }
        }
        LearnerCurriculum curriculum = curricula.find(userId);
        curriculum.reorder(topics.stream().map(LearningContentClient.Topic::topicId).toList());
        curricula.save(curriculum);
        catalog.upsert(entries);
        var statuses = curriculum.statuses();
        var counts = lessons.completedCountsByTopic(userId);
        List<TopicResult> results = new ArrayList<>();
        for (int index = 0; index < topics.size(); index++) {
            var topic = topics.get(index);
            results.add(new TopicResult(topic.topicId(), topic.code(), topic.name(), index + 1,
                    statuses.get(topic.topicId()), counts.getOrDefault(topic.topicId(), 0),
                    topic.requiredFeatureKey()));
        }
        return List.copyOf(results);
    }
}
