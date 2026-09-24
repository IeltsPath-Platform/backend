package com.group01.assessment.infrastructure.persistence.repository;

import com.group01.assessment.infrastructure.persistence.entity.ErrorAnalysisItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ErrorAnalysisItemJpaRepository extends JpaRepository<ErrorAnalysisItemJpaEntity, UUID> {
    @Query("select errorItem from ErrorAnalysisItemJpaEntity errorItem " +
            "join ItemResultJpaEntity itemResult on itemResult.id = errorItem.itemResultId " +
            "where itemResult.resultId = :resultId")
    List<ErrorAnalysisItemJpaEntity> findByResultId(@Param("resultId") UUID resultId);
}
