package com.ieltspath.library.infrastructure.persistence.mapper;

import com.ieltspath.library.domain.aggregate.VideoDictationAttempt;
import com.ieltspath.library.infrastructure.persistence.entity.VideoDictationAttemptJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VideoDictationAttemptMapper {
    void copy(VideoDictationAttempt source, @MappingTarget VideoDictationAttemptJpaEntity target);

    VideoDictationAttempt toDomain(VideoDictationAttemptJpaEntity entity);
}
