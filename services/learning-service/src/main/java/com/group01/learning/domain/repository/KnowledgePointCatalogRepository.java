package com.group01.learning.domain.repository;

import com.group01.learning.domain.vo.KnowledgePointCatalogEntry;

import java.util.List;

/** The catalog of knowledge points shared by all learners, refreshed from Content. */
public interface KnowledgePointCatalogRepository {
    void upsert(List<KnowledgePointCatalogEntry> entries);
}
