package com.group01.learningsupport.infrastructure.persistence.mapper;

import com.group01.learningsupport.domain.aggregate.Streak;
import com.group01.learningsupport.infrastructure.persistence.entity.StreakJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface StreakMapper {
    void copy(Streak source, @MappingTarget StreakJpaEntity target);

    Streak toDomain(StreakJpaEntity entity);
}
