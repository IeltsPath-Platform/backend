package com.ieltspath.content.infrastructure.persistence.repository;

import com.ieltspath.content.infrastructure.persistence.entity.CourseJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseJpaRepository extends JpaRepository<CourseJpaEntity, UUID> {
    boolean existsByCode(String code);
    Optional<CourseJpaEntity> findByBandLevel(BigDecimal bandLevel);
    List<CourseJpaEntity> findAllByOrderByBandLevelAsc();

    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM topics WHERE course_id = :id)
                OR EXISTS (SELECT 1 FROM content_packages WHERE course_id = :id)
                OR EXISTS (SELECT 1 FROM course_practice_test_milestones WHERE course_id = :id)
            """, nativeQuery = true)
    boolean hasContent(@Param("id") UUID id);
}
