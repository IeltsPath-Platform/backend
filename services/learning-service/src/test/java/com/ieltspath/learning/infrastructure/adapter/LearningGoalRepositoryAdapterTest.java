package com.ieltspath.learning.infrastructure.adapter;

import com.ieltspath.learning.domain.exception.LearningGoalInvariantViolationException;
import com.ieltspath.learning.domain.vo.GoalStatus;
import com.ieltspath.learning.infrastructure.persistence.entity.LearningGoalJpaEntity;
import com.ieltspath.learning.infrastructure.persistence.mapper.LearningGoalMapper;
import com.ieltspath.learning.infrastructure.persistence.repository.LearningGoalJpaRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LearningGoalRepositoryAdapterTest {
    @Test
    void activeGoalLookupFailsClosedWhenDuplicateActiveRowsExist() {
        UUID userId = UUID.randomUUID();
        LearningGoalJpaRepository jpaRepository = mock(LearningGoalJpaRepository.class);
        LearningGoalMapper mapper = mock(LearningGoalMapper.class);
        LearningGoalRepositoryAdapter adapter = new LearningGoalRepositoryAdapter(
                jpaRepository, mapper
        );
        when(jpaRepository.findAllByUserIdAndStatusOrderByCreatedAtDescIdDesc(userId, GoalStatus.ACTIVE))
                .thenReturn(List.of(new LearningGoalJpaEntity(), new LearningGoalJpaEntity()));

        assertThrows(LearningGoalInvariantViolationException.class, () -> adapter.findActiveByUserId(userId));
    }
}
