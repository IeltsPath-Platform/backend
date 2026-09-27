package com.group01.content.infrastructure.persistence.adapter;

import com.group01.content.domain.aggregate.Topic;
import com.group01.content.domain.repository.TopicRepository;
import com.group01.content.infrastructure.persistence.mapper.TopicPersistenceMapper;
import com.group01.content.infrastructure.persistence.repository.TopicJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TopicRepositoryAdapter implements TopicRepository {

    private final TopicJpaRepository topicJpaRepository;
    private final TopicPersistenceMapper mapper;

    public TopicRepositoryAdapter(TopicJpaRepository topicJpaRepository, TopicPersistenceMapper mapper) {
        this.topicJpaRepository = topicJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Topic save(Topic topic) {
        var entity = mapper.toEntity(topic);
        var saved = topicJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Topic> findById(UUID id) {
        return topicJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Topic> findByCode(String code) {
        return topicJpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<Topic> findAll() {
        return topicJpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Topic> findAllByIds(Collection<UUID> ids) {
        return topicJpaRepository.findAllById(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Topic> findRootTopics() {
        return topicJpaRepository.findByParentTopicIdIsNullOrderBySortOrderAsc().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Topic> findByParentTopicId(UUID parentTopicId) {
        return topicJpaRepository.findByParentTopicIdOrderBySortOrderAsc(parentTopicId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return topicJpaRepository.existsByCode(code);
    }
}

