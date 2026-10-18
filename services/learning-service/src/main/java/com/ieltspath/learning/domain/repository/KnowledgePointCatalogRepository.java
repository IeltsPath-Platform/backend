package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.vo.KnowledgePointCatalogEntry;

import java.util.List;

/** The catalog of knowledge points shared by all learners, refreshed from Content. */
public interface KnowledgePointCatalogRepository {
    void upsert(List<KnowledgePointCatalogEntry> entries);

    /** Every point last seen in Content, with the skill and course band it carried then. */
    List<KnowledgePointCatalogEntry> findAll();
}
