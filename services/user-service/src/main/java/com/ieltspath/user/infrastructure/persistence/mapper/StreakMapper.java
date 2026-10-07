package com.ieltspath.user.infrastructure.persistence.mapper;

import com.ieltspath.user.domain.aggregate.Streak;
import com.ieltspath.user.infrastructure.persistence.entity.StreakJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface StreakMapper {
    void copy(Streak source, @MappingTarget StreakJpaEntity target);

    Streak toDomain(StreakJpaEntity entity);
}
