package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.vo.RemediationFlag;

import java.util.List;
import java.util.UUID;

public interface RemediationFlagRepository {
    /** One flag per source attempt and kind; a second insert for the same pair changes nothing. */
    void insert(RemediationFlag flag);

    /** Oldest first, each with whether any of its reviews is still PENDING. */
    List<RemediationFlag> findByCourse(UUID userId, UUID courseId);
}
