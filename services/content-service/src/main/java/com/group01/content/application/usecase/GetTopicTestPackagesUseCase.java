package com.group01.content.application.usecase;

import com.group01.content.application.port.LearningContentReader;
import com.group01.content.application.result.TopicTestPackageResult;
import com.group01.content.domain.exception.TopicNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetTopicTestPackagesUseCase {
    private final LearningContentReader reader;

    public GetTopicTestPackagesUseCase(LearningContentReader reader) {
        this.reader = reader;
    }

    public List<TopicTestPackageResult> execute(UUID topicId) {
        if (!reader.activeTopicExists(topicId)) {
            throw new TopicNotFoundException(topicId);
        }
        return reader.publishedTestPackages(topicId);
    }
}
