package com.group01.assessment.infrastructure.persistence.repository;

import com.group01.assessment.infrastructure.persistence.entity.AssessmentResultJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentResultJpaRepository extends JpaRepository<AssessmentResultJpaEntity, UUID> {
    Optional<AssessmentResultJpaEntity> findTopByAttemptIdOrderByResultVersionDesc(UUID attemptId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select result from AssessmentResultJpaEntity result where result.attemptId = :attemptId "
            + "order by result.resultVersion desc")
    List<AssessmentResultJpaEntity> findForUpdateByAttemptId(
            @Param("attemptId") UUID attemptId, Pageable pageable);
}
