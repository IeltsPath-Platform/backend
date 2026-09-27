package com.group01.content.application.usecase;

import com.group01.content.application.command.UpdateTopicCommand;
import com.group01.content.application.result.TopicResult;
import com.group01.content.domain.aggregate.Topic;
import com.group01.content.domain.exception.TopicNotFoundException;
import com.group01.content.domain.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateTopicUseCase {

    private final TopicRepository topicRepository;

    public UpdateTopicUseCase(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    public TopicResult execute(UpdateTopicCommand command) {
        Topic topic = topicRepository.findById(command.id())
                .orElseThrow(() -> new TopicNotFoundException(command.id()));

        if (command.parentTopicId() != null && !command.parentTopicId().equals(topic.getParentTopicId())) {
            topicRepository.findById(command.parentTopicId())
                    .orElseThrow(() -> new TopicNotFoundException(command.parentTopicId()));
        }

        topic.update(command.parentTopicId(), command.name(), command.sortOrder(), command.status(),
                command.band());
        Topic saved = topicRepository.save(topic);

        return new TopicResult(
                saved.getId(),
                saved.getParentTopicId(),
                saved.getCode(),
                saved.getName(),
                saved.getSortOrder(),
                saved.getStatus(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                saved.getBand()
        );
    }
}

