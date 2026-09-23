package com.group01.assessment.infrastructure.persistence.repository;

import com.group01.assessment.infrastructure.persistence.entity.AttemptItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttemptItemJpaRepository extends JpaRepository<AttemptItemJpaEntity, UUID> {
    List<AttemptItemJpaEntity> findByAttemptSectionIdOrderBySortOrderAsc(UUID sectionId);
    @Query("select i from AttemptItemJpaEntity i join AttemptSectionJpaEntity s on s.id = i.attemptSectionId where i.id = :itemId and s.attemptId = :attemptId")
    Optional<AttemptItemJpaEntity> findByIdAndAttemptId(@Param("itemId") UUID itemId, @Param("attemptId") UUID attemptId);
    @Query("select i from AttemptItemJpaEntity i join AttemptSectionJpaEntity s on s.id = i.attemptSectionId where s.attemptId = :attemptId order by s.sortOrder, i.sortOrder")
    List<AttemptItemJpaEntity> findByAttemptId(@Param("attemptId") UUID attemptId);
}
