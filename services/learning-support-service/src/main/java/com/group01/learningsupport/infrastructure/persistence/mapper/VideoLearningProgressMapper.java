package com.group01.learningsupport.infrastructure.persistence.mapper;

import com.group01.learningsupport.domain.aggregate.VideoLearningProgress;
import com.group01.learningsupport.infrastructure.persistence.entity.VideoLearningProgressJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VideoLearningProgressMapper {
    void copy(VideoLearningProgress source, @MappingTarget VideoLearningProgressJpaEntity target);

    VideoLearningProgress toDomain(VideoLearningProgressJpaEntity entity);
}
