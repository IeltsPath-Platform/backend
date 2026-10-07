package com.ieltspath.library.infrastructure.persistence.mapper;

import com.ieltspath.library.domain.aggregate.VideoLearningProgress;
import com.ieltspath.library.infrastructure.persistence.entity.VideoLearningProgressJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VideoLearningProgressMapper {
    void copy(VideoLearningProgress source, @MappingTarget VideoLearningProgressJpaEntity target);

    VideoLearningProgress toDomain(VideoLearningProgressJpaEntity entity);
}
