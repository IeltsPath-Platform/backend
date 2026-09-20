package com.group01.user.infrastructure.persistence.mapper;

import com.group01.user.domain.aggregate.OAuthIdentity;
import com.group01.user.infrastructure.persistence.entity.OAuthIdentityJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OAuthIdentityMapper {
    @Mapping(target = "userId", source = "user.id")
    OAuthIdentity toDomain(OAuthIdentityJpaEntity entity);

    @Mapping(target = "user", ignore = true)
    OAuthIdentityJpaEntity toEntity(OAuthIdentity domain);
}

