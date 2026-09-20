package com.group01.user.infrastructure.persistence.mapper;

import com.group01.user.domain.aggregate.LearnerProfile;
import com.group01.user.infrastructure.persistence.entity.LearnerProfileJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LearnerProfileMapper {
    @Mapping(target = "visibility", source = "profileVisibility")
    LearnerProfile toDomain(LearnerProfileJpaEntity entity);

    @Mapping(target = "profileVisibility", source = "visibility")
    @Mapping(target = "user", ignore = true)
    LearnerProfileJpaEntity toEntity(LearnerProfile domain);
}

