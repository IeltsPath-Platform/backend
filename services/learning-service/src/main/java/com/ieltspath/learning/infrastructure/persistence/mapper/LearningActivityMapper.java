package com.ieltspath.learning.infrastructure.persistence.mapper;

import com.ieltspath.learning.domain.aggregate.LearningActivity;
import com.ieltspath.learning.infrastructure.persistence.entity.LearningActivityJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LearningActivityMapper {
    void copy(LearningActivity source, @MappingTarget LearningActivityJpaEntity target);

    LearningActivity toDomain(LearningActivityJpaEntity entity);
}
