package com.ieltspath.content.domain.repository;

import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.QuestionPurpose;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository {
    Question save(Question question);
    Optional<Question> findById(UUID id);
    Optional<Question> findByIdForUpdate(UUID id);
    List<Question> findByIds(List<UUID> ids);
    /**
     * One page of catalog metadata, newest first. Every filter is optional and applied in the database; versions are
     * not needed by the list response. {@code page} is zero-based.
     */
    Page findPage(Skill skill, QuestionPurpose purpose, PublicationStatus status, int page, int size);
    /** The question carrying this import reference, if any. Matched exactly. */
    Optional<Question> findByImportRef(String importRef);

    record Page(long totalElements, List<Question> items) {}
}

