package com.group01.user.infrastructure.persistence.mapper;

import com.group01.user.domain.aggregate.LearningGoal;
import com.group01.user.infrastructure.persistence.entity.LearningGoalJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LearningGoalMapper {
    @Mapping(target = "userId", source = "user.id")
    LearningGoal toDomain(LearningGoalJpaEntity entity);

    @Mapping(target = "user", ignore = true)
    LearningGoalJpaEntity toEntity(LearningGoal domain);
}

