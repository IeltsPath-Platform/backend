package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.vo.PracticePassReason;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface LessonPracticePassRepository {
    Map<UUID, PracticePassReason> findByLessons(UUID userId, Collection<UUID> lessonIds);
    void insertIfAbsent(UUID userId, Map<UUID, PracticePassReason> reasons);
}
