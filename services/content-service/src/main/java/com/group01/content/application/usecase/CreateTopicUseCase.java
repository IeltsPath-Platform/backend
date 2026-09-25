package com.group01.content.application.usecase;

import com.group01.content.application.command.CreateTopicCommand;
import com.group01.content.application.result.TopicResult;
import com.group01.content.domain.aggregate.Topic;
import com.group01.content.domain.exception.DuplicateCodeException;
import com.group01.content.domain.exception.TopicNotFoundException;
import com.group01.content.domain.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateTopicUseCase {

    private final TopicRepository topicRepository;

    public CreateTopicUseCase(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    public TopicResult execute(CreateTopicCommand command) {
        if (topicRepository.existsByCode(command.code())) {
            throw new DuplicateCodeException("Topic", command.code());
        }
        if (command.parentTopicId() != null) {
            topicRepository.findById(command.parentTopicId())
                    .orElseThrow(() -> new TopicNotFoundException(command.parentTopicId()));
        }

        Topic topic = Topic.create(
                command.parentTopicId(),
                command.code(),
                command.name(),
                command.sortOrder(),
                command.band()
        );

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

