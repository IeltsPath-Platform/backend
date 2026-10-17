package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Code, name, skill and course band of each knowledge point, read from Content's topic sequence. */
@Component
public class KnowledgePointDirectory {
    private final LearningContentClient content;

    public KnowledgePointDirectory(LearningContentClient content) {
        this.content = content;
    }

    /** A knowledge point listed in several topics takes the first one. */
    public Map<UUID, Entry> describe() {
        Map<UUID, Entry> described = new HashMap<>();
        for (LearningContentClient.Topic topic : content.getTopicSequence()) {
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
