package com.ieltspath.user.infrastructure.persistence.mapper;

import com.ieltspath.user.domain.aggregate.LearningActivity;
import com.ieltspath.user.infrastructure.persistence.entity.LearningActivityJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LearningActivityMapper {
    void copy(LearningActivity source, @MappingTarget LearningActivityJpaEntity target);

    LearningActivity toDomain(LearningActivityJpaEntity entity);
}
