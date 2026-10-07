package com.ieltspath.content.domain.repository;

import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.QuestionPurpose;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository {
    Question save(Question question);
    Optional<Question> findById(UUID id);
    List<Question> findByIds(List<UUID> ids);
    List<Question> findBySkill(Skill skill);
    /** Catalog metadata filtered in the database; versions are not needed by the list response. */
    List<Question> findByPurpose(QuestionPurpose purpose, Skill skill);
    List<Question> findAll();
}

