package com.group01.content.domain.repository;

import com.group01.content.domain.aggregate.Question;
import com.group01.content.domain.vo.Skill;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository {
    Question save(Question question);
    Optional<Question> findById(UUID id);
    List<Question> findBySkill(Skill skill);
    List<Question> findAll();
}

