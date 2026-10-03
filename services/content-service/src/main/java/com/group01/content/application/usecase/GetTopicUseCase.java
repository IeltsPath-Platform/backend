package com.group01.content.application.usecase;

import com.group01.content.application.result.TopicResult;
import com.group01.content.domain.aggregate.Topic;
import com.group01.content.domain.exception.TopicNotFoundException;
import com.group01.content.domain.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetTopicUseCase {
    private final TopicRepository topicRepository;

    public GetTopicUseCase(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    public TopicResult execute(UUID id) {
        Topic topic = topicRepository.findById(id).orElseThrow(() -> new TopicNotFoundException(id));
        return new TopicResult(topic.getId(), topic.getParentTopicId(), topic.getCode(), topic.getName(),
                topic.getSortOrder(), topic.getStatus(), topic.getCreatedAt(), topic.getUpdatedAt(), topic.getBand(),
                topic.getSkill());
    }
}
