package com.group01.content.infrastructure.persistence.repository;

import com.group01.content.infrastructure.persistence.entity.CourseJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseJpaRepository extends JpaRepository<CourseJpaEntity, UUID> {
    boolean existsByCode(String code);
    Optional<CourseJpaEntity> findByBandLevel(BigDecimal bandLevel);
    List<CourseJpaEntity> findAllByOrderByBandLevelAsc();
}
