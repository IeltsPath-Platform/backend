package com.group01.learningsupport.infrastructure.persistence.mapper;

import com.group01.learningsupport.domain.aggregate.LearningActivity;
import com.group01.learningsupport.infrastructure.persistence.entity.LearningActivityJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LearningActivityMapper {
    void copy(LearningActivity source, @MappingTarget LearningActivityJpaEntity target);

    LearningActivity toDomain(LearningActivityJpaEntity entity);
}
