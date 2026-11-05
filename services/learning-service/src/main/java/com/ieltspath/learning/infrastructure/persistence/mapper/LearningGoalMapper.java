package com.ieltspath.learning.infrastructure.persistence.mapper;

import com.ieltspath.learning.domain.aggregate.LearningGoal;
import com.ieltspath.learning.infrastructure.persistence.entity.LearningGoalJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LearningGoalMapper {
    LearningGoal toDomain(LearningGoalJpaEntity entity);

    LearningGoalJpaEntity toEntity(LearningGoal domain);
}

