package com.group01.content.application.usecase;

import com.group01.content.application.port.LearningContentReader;
import com.group01.content.application.result.TopicSequenceResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** The learning order: topics a learner can study and test, each with the knowledge points it measures. */
@Service
@Transactional(readOnly = true)
public class GetTopicSequenceUseCase {
    private final LearningContentReader reader;

    public GetTopicSequenceUseCase(LearningContentReader reader) {
        this.reader = reader;
    }

    public List<TopicSequenceResult> execute() {
        // Same threshold as a default practice-set search, so hasPracticeSet and search never disagree.
        return reader.topicSequence(SearchPracticeSetsUseCase.DEFAULT_MIN_QUESTIONS);
    }
}
