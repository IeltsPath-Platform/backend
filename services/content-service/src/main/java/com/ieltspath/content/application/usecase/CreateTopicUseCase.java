package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateTopicCommand;
import com.ieltspath.content.application.result.TopicResult;
import com.ieltspath.content.domain.aggregate.Topic;
import com.ieltspath.content.domain.exception.DuplicateCodeException;
import com.ieltspath.content.domain.exception.TopicNotFoundException;
import com.ieltspath.content.domain.repository.TopicRepository;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.exception.CourseNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateTopicUseCase {

    private final TopicRepository topicRepository;
    private final CourseRepository courseRepository;

    public CreateTopicUseCase(TopicRepository topicRepository, CourseRepository courseRepository) {
        this.topicRepository = topicRepository;
        this.courseRepository = courseRepository;
    }

    public TopicResult execute(CreateTopicCommand command) {
        if (topicRepository.existsByCode(command.code())) {
            throw new DuplicateCodeException("Topic", command.code());
        }
        if (command.parentTopicId() != null) {
            topicRepository.findById(command.parentTopicId())
                    .orElseThrow(() -> new TopicNotFoundException(command.parentTopicId()));
        }

        if (command.courseId() != null) {
            courseRepository.findById(command.courseId())
                    .orElseThrow(() -> new CourseNotFoundException(command.courseId()));
        }

        Topic topic = Topic.create(
                command.parentTopicId(),
                command.code(),
                command.name(),
                command.sortOrder(),
                command.band(),
                command.skill()
        );
        if (command.courseId() != null) topic.assignCourse(command.courseId());

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
                saved.getBand(),
                saved.getSkill(),
                saved.getCourseId()
        );
    }
}

