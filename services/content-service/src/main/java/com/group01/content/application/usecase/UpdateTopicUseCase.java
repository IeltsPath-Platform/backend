package com.group01.content.application.usecase;

import com.group01.content.application.command.UpdateTopicCommand;
import com.group01.content.application.result.TopicResult;
import com.group01.content.domain.aggregate.Topic;
import com.group01.content.domain.exception.TopicNotFoundException;
import com.group01.content.domain.repository.TopicRepository;
import com.group01.content.domain.repository.CourseRepository;
import com.group01.content.domain.exception.CourseNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateTopicUseCase {

    private final TopicRepository topicRepository;
    private final CourseRepository courseRepository;

    public UpdateTopicUseCase(TopicRepository topicRepository) {
        this(topicRepository, null);
    }

    @Autowired
    public UpdateTopicUseCase(TopicRepository topicRepository, CourseRepository courseRepository) {
        this.topicRepository = topicRepository;
        this.courseRepository = courseRepository;
    }

    public TopicResult execute(UpdateTopicCommand command) {
        Topic topic = topicRepository.findById(command.id())
                .orElseThrow(() -> new TopicNotFoundException(command.id()));

        if (command.parentTopicId() != null && !command.parentTopicId().equals(topic.getParentTopicId())) {
            topicRepository.findById(command.parentTopicId())
                    .orElseThrow(() -> new TopicNotFoundException(command.parentTopicId()));
        }

        if (command.courseId() != null) {
            courseRepository.findById(command.courseId())
                    .orElseThrow(() -> new CourseNotFoundException(command.courseId()));
        }

        topic.update(command.parentTopicId(), command.name(), command.sortOrder(), command.status(),
                command.band());
        if (command.skill() != null && command.skill() != topic.getSkill()) {
            topic.changeSkill(command.skill());
        }
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

