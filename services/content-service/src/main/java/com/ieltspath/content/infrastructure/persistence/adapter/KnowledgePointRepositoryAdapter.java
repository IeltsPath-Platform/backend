package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.exception.DuplicateCodeException;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.infrastructure.persistence.mapper.KnowledgePointPersistenceMapper;
import com.ieltspath.content.infrastructure.persistence.repository.KnowledgePointJpaRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class KnowledgePointRepositoryAdapter implements KnowledgePointRepository {
    private static final String CODE_UNIQUE_CONSTRAINT = "knowledge_points_code_key";

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
        try {
            var saved = knowledgePointJpaRepository.saveAndFlush(entity);
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException exception) {
            if (isCodeUniqueConstraintViolation(exception)) {
                throw new DuplicateCodeException("KnowledgePoint", kp.getCode());
            }
            throw exception;
        }
    }

    @Override
    public Optional<KnowledgePoint> findById(UUID id) {
        return knowledgePointJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<KnowledgePoint> findByIds(Collection<UUID> ids) {
        return knowledgePointJpaRepository.findAllById(ids).stream().map(mapper::toDomain).toList();
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

    @Override
    public boolean isInUse(UUID id) {
        return knowledgePointJpaRepository.isInUse(id);
    }

    private boolean isCodeUniqueConstraintViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                return CODE_UNIQUE_CONSTRAINT.equals(violation.getConstraintName());
            }
            cause = cause.getCause();
        }
        return false;
    }
}

