package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.domain.exception.LearningGoalInvariantViolationException;
import com.ieltspath.user.domain.vo.GoalStatus;
import com.ieltspath.user.infrastructure.persistence.entity.LearningGoalJpaEntity;
import com.ieltspath.user.infrastructure.persistence.mapper.LearningGoalMapper;
import com.ieltspath.user.infrastructure.persistence.repository.LearningGoalJpaRepository;
import com.ieltspath.user.infrastructure.persistence.repository.UserJpaRepository;
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
                jpaRepository, mapper, mock(UserJpaRepository.class)
        );
        when(jpaRepository.findAllByUser_IdAndStatusOrderByCreatedAtDescIdDesc(userId, GoalStatus.ACTIVE))
                .thenReturn(List.of(new LearningGoalJpaEntity(), new LearningGoalJpaEntity()));

        assertThrows(LearningGoalInvariantViolationException.class, () -> adapter.findActiveByUserId(userId));
    }
}
