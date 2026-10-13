package com.ieltspath.content.infrastructure.persistence.repository;

import com.ieltspath.content.infrastructure.persistence.entity.LessonResourceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LessonResourceJpaRepository extends JpaRepository<LessonResourceJpaEntity, UUID> {
    List<LessonResourceJpaEntity> findByLessonIdOrderByResourceTypeAscSortOrderAsc(UUID lessonId);
}
