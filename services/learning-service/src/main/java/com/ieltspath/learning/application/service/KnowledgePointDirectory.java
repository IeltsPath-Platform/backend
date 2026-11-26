package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.TopicCatalog;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Code, name, skill and course band of each knowledge point, read from Content's topic sequence. */
@Component
public class KnowledgePointDirectory {
    private final LearningContentClient content;
    private final TopicCatalog topicCatalog;

    public KnowledgePointDirectory(LearningContentClient content, TopicCatalog topicCatalog) {
        this.content = content;
        this.topicCatalog = topicCatalog;
    }

    /** A knowledge point listed in several topics takes the first one. */
    public Map<UUID, Entry> describe() {
        return describeTopics(content.getTopicSequence());
    }

    /** Optionally includes error-trap children for learner reports and the knowledge tree. */
    public Map<UUID, Entry> describe(boolean includeErrorTraps) {
        return describeTopics(includeErrorTraps ? content.getTopicSequence(true) : content.getTopicSequence());
    }

    /** Display-only names and descriptions may use the short-lived shared topic cache. */
    public Map<UUID, Entry> describeForDisplay() {
        return describeForDisplay(false);
    }

    public Map<UUID, Entry> describeForDisplay(boolean includeErrorTraps) {
        return describeTopics(topicCatalog.topics(includeErrorTraps));
    }

    private Map<UUID, Entry> describeTopics(java.util.List<LearningContentClient.Topic> topics) {
        Map<UUID, Entry> described = new HashMap<>();
        if (topics == null) return described;
        for (LearningContentClient.Topic topic : topics) {
            BigDecimal band = topic.course() == null ? null : topic.course().bandLevel();
            for (LearningContentClient.KnowledgePoint point : topic.knowledgePoints()) {
                described.putIfAbsent(point.id(), new Entry(point.code(), point.name(), point.skill(), band));
            }
        }
        return described;
    }

    public record Entry(String code, String name, String skill, BigDecimal band) {
        public static final Entry UNKNOWN = new Entry(null, null, null, null);
    }
}
