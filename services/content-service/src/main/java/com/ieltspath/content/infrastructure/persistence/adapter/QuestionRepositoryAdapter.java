package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.infrastructure.persistence.mapper.QuestionPersistenceMapper;
import com.ieltspath.content.infrastructure.persistence.repository.QuestionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class QuestionRepositoryAdapter implements QuestionRepository {

    private final QuestionJpaRepository questionJpaRepository;
    private final QuestionPersistenceMapper mapper;
    private final JdbcTemplate jdbcTemplate;

    public QuestionRepositoryAdapter(QuestionJpaRepository questionJpaRepository,
                                     QuestionPersistenceMapper mapper,
                                     JdbcTemplate jdbcTemplate) {
        this.questionJpaRepository = questionJpaRepository;
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Question save(Question question) {
        var entity = mapper.toEntity(question);
        var saved = questionJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Question> findById(UUID id) {
        return questionJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Question> findByIdForUpdate(UUID id) {
        var rows = jdbcTemplate.query("SELECT id FROM questions WHERE id = ? FOR UPDATE",
                (rs, row) -> rs.getObject("id", UUID.class), id);
        return rows.isEmpty() ? Optional.empty() : findById(id);
    }

    @Override
    public List<Question> findByIds(List<UUID> ids) {
        return questionJpaRepository.findAllByIdIn(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Question> findBySkill(Skill skill) {
        return questionJpaRepository.findBySkill(skill).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Question> findAll() {
        return questionJpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Question> findByPurpose(QuestionPurpose purpose, Skill skill) {
        return questionJpaRepository.findByPurpose(purpose, skill).stream().map(mapper::toDomainSummary).toList();
    }
}

