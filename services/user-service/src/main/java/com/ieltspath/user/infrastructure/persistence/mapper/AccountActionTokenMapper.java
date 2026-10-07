package com.ieltspath.user.infrastructure.persistence.mapper;

import com.ieltspath.user.domain.aggregate.AccountActionToken;
import com.ieltspath.user.infrastructure.persistence.entity.AccountActionTokenJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountActionTokenMapper {
    @Mapping(target = "userId", source = "user.id")
    AccountActionToken toDomain(AccountActionTokenJpaEntity entity);

    @Mapping(target = "user", ignore = true)
    AccountActionTokenJpaEntity toEntity(AccountActionToken domain);
}

