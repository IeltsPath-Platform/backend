package com.group01.content.infrastructure.persistence.adapter;

import com.group01.content.domain.aggregate.Question;
import com.group01.content.domain.repository.QuestionRepository;
import com.group01.content.domain.vo.Skill;
import com.group01.content.infrastructure.persistence.mapper.QuestionPersistenceMapper;
import com.group01.content.infrastructure.persistence.repository.QuestionJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class QuestionRepositoryAdapter implements QuestionRepository {

    private final QuestionJpaRepository questionJpaRepository;
    private final QuestionPersistenceMapper mapper;

    public QuestionRepositoryAdapter(QuestionJpaRepository questionJpaRepository,
                                     QuestionPersistenceMapper mapper) {
        this.questionJpaRepository = questionJpaRepository;
        this.mapper = mapper;
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
    public List<Question> findBySkill(Skill skill) {
        return questionJpaRepository.findBySkill(skill).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Question> findAll() {
        return questionJpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}

