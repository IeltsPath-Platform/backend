package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.TopicCatalog;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgePointDirectoryTest {
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final TopicCatalog topics = mock(TopicCatalog.class);
    private final KnowledgePointDirectory directory = new KnowledgePointDirectory(content, topics);

    @Test
    void describeReadsCurrentContentWithoutUsingTheDisplayCache() {
        var topic = topic("direct-name");
        when(content.getTopicSequence()).thenReturn(List.of(topic));

        var described = directory.describe();

        assertThat(described.get(topic.knowledgePoints().getFirst().id()).name()).isEqualTo("direct-name");
        verify(content).getTopicSequence();
    }

    @Test
    void displayDescriptionsUseTheCatalogWithTheRequestedErrorTrapVariant() {
        var base = topic("base-name");
        var traps = topic("trap-name");
        when(topics.topics(false)).thenReturn(List.of(base));
        when(topics.topics(true)).thenReturn(List.of(traps));

        var baseDescription = directory.describeForDisplay();
        var trapDescription = directory.describeForDisplay(true);
        assertThat(baseDescription.values()).extracting(KnowledgePointDirectory.Entry::name)
                .containsExactly("base-name");
        assertThat(trapDescription.values()).extracting(KnowledgePointDirectory.Entry::name)
                .containsExactly("trap-name");
        verify(topics, org.mockito.Mockito.times(1)).topics(false);
        verify(topics, org.mockito.Mockito.times(1)).topics(true);
    }

    private static LearningContentClient.Topic topic(String name) {
        UUID topicId = UUID.randomUUID();
        UUID pointId = UUID.randomUUID();
        var course = new LearningContentClient.Course(UUID.randomUUID(), "COURSE", "Course",
                new BigDecimal("6.0"), true);
        var point = new LearningContentClient.KnowledgePoint(pointId, "KP", name, "LESSON", "READING",
                "description", true, "B1", "can do", UUID.randomUUID(), "TRAP", 2);
        return new LearningContentClient.Topic(topicId, "TOPIC", "Topic", 1, "PREMIUM", List.of(point),
                LearningSkill.READING, true, course, Set.of(LearningSkill.READING), true, 3, "topic description");
    }
}
