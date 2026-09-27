package com.group01.content.domain.repository;

import com.group01.content.domain.aggregate.Topic;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicRepository {
    Topic save(Topic topic);
    Optional<Topic> findById(UUID id);
    Optional<Topic> findByCode(String code);
    List<Topic> findAll();

    List<Topic> findAllByIds(Collection<UUID> ids);
    List<Topic> findRootTopics();
    List<Topic> findByParentTopicId(UUID parentTopicId);
    boolean existsByCode(String code);
}

