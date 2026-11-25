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
    List<Question> findByVersionIdsForUpdate(List<UUID> versionIds);
    /**
     * One page of catalog metadata, newest first. Every filter is optional and applied in the database; versions are
     * not needed by the list response. {@code page} is zero-based.
     */
    Page findPage(Skill skill, QuestionPurpose purpose, PublicationStatus status, String code, int page, int size);
    Page findPageByKnowledgePoints(Skill skill, QuestionPurpose purpose, PublicationStatus status, String code,
                                  List<UUID> knowledgePointIds, int page, int size);
    Page findFreePage(Skill skill, QuestionPurpose purpose, PublicationStatus status, String code, UUID exceptBlockId, int page, int size);
    Page findFreePageByKnowledgePoints(Skill skill, QuestionPurpose purpose, PublicationStatus status, String code,
                                     UUID exceptBlockId, List<UUID> knowledgePointIds, int page, int size);
    List<VersionStem> findVersionStems(List<UUID> versionIds);
    List<VersionKnowledgePoint> findVersionKnowledgePoints(List<UUID> versionIds);
    default Page findPage(Skill skill, QuestionPurpose purpose, PublicationStatus status, int page, int size) {
        return findPage(skill, purpose, status, null, page, size);
    }
    /** The question carrying this import reference, if any. Matched exactly. */
    Optional<Question> findByImportRef(String importRef);
    boolean existsByCode(String code);

    record Page(long totalElements, List<Question> items) {}
    record VersionStem(UUID versionId, String stem) {}
    record VersionKnowledgePoint(UUID versionId, String code, String name, int total) {}
}

