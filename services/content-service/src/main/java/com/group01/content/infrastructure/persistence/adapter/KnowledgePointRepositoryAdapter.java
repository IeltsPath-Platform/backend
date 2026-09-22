package com.group01.content.infrastructure.persistence.adapter;

import com.group01.content.domain.aggregate.KnowledgePoint;
import com.group01.content.domain.repository.KnowledgePointRepository;
import com.group01.content.infrastructure.persistence.mapper.KnowledgePointPersistenceMapper;
import com.group01.content.infrastructure.persistence.repository.KnowledgePointJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class KnowledgePointRepositoryAdapter implements KnowledgePointRepository {

    private final KnowledgePointJpaRepository knowledgePointJpaRepository;
    private final KnowledgePointPersistenceMapper mapper;

    public KnowledgePointRepositoryAdapter(KnowledgePointJpaRepository knowledgePointJpaRepository,
                                           KnowledgePointPersistenceMapper mapper) {
        this.knowledgePointJpaRepository = knowledgePointJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public KnowledgePoint save(KnowledgePoint kp) {
        var entity = mapper.toEntity(kp);
        var saved = knowledgePointJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<KnowledgePoint> findById(UUID id) {
        return knowledgePointJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<KnowledgePoint> findByCode(String code) {
        return knowledgePointJpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<KnowledgePoint> findByTopicId(UUID topicId) {
        return knowledgePointJpaRepository.findByTopicId(topicId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<KnowledgePoint> findAll() {
        return knowledgePointJpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return knowledgePointJpaRepository.existsByCode(code);
    }
}

