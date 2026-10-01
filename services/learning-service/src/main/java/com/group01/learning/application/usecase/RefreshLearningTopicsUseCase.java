package com.group01.learning.application.usecase;

import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.port.LearningProgressStore;
import com.group01.learning.application.port.LearningProgressStore.TopicOrder;
import com.group01.learning.application.result.TopicResult;
import com.group01.learning.domain.service.TopicStatusDeriver;
import com.group01.learning.domain.vo.KnowledgePointCatalogEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class RefreshLearningTopicsUseCase {
    private final LearningContentClient content;
    private final LearningProgressStore store;
    private final TopicStatusDeriver statusDeriver = new TopicStatusDeriver();

    public RefreshLearningTopicsUseCase(LearningContentClient content, LearningProgressStore store) {
        this.content = content;
        this.store = store;
    }

    @Transactional
    public List<TopicResult> execute(UUID userId) {
        store.lockUser(userId);
        var topics = content.getTopicSequence().stream().sorted(Comparator
                .comparingInt(LearningContentClient.Topic::sortOrder)
                .thenComparing(topic -> topic.topicId().toString())).toList();
        List<TopicOrder> orders = new ArrayList<>();
        List<KnowledgePointCatalogEntry> catalog = new ArrayList<>();
        for (int index = 0; index < topics.size(); index++) {
            var topic = topics.get(index);
            orders.add(new TopicOrder(topic.topicId(), index + 1));
            for (var kp : topic.knowledgePoints()) {
                catalog.add(new KnowledgePointCatalogEntry(kp.id(), topic.topicId(), kp.hasPracticeSet()));
            }
        }
        store.refreshCurriculum(userId, orders, catalog);
        var statuses = statusDeriver.derive(store.findTopics(userId));
        var counts = store.completedLessonCounts(userId);
        List<TopicResult> results = new ArrayList<>();
        for (int index = 0; index < topics.size(); index++) {
            var topic = topics.get(index);
            results.add(new TopicResult(topic.topicId(), topic.code(), topic.name(), index + 1,
                    statuses.get(topic.topicId()), counts.getOrDefault(topic.topicId(), 0)));
        }
        return List.copyOf(results);
    }
}
