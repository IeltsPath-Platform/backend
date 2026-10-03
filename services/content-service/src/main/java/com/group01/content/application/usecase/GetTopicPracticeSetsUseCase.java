package com.group01.content.application.usecase;

import com.group01.content.application.port.LearningContentReader;
import com.group01.content.application.result.LessonPracticeSetsResult;
import com.group01.content.domain.exception.TopicNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** The Practice of every published lesson of a topic, read at once. */
@Service
@Transactional(readOnly = true)
public class GetTopicPracticeSetsUseCase {
    private final LearningContentReader reader;

    public GetTopicPracticeSetsUseCase(LearningContentReader reader) {
        this.reader = reader;
    }

    public List<LessonPracticeSetsResult> execute(UUID topicId) {
        if (!reader.activeTopicExists(topicId)) {
            throw new TopicNotFoundException(topicId);
        }
        return reader.topicPracticeSets(topicId);
    }
}
