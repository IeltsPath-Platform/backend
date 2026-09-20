package com.group01.user.infrastructure.persistence.mapper;

import com.group01.user.domain.aggregate.AccountActionToken;
import com.group01.user.infrastructure.persistence.entity.AccountActionTokenJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountActionTokenMapper {
    @Mapping(target = "userId", source = "user.id")
    AccountActionToken toDomain(AccountActionTokenJpaEntity entity);

    @Mapping(target = "user", ignore = true)
    AccountActionTokenJpaEntity toEntity(AccountActionToken domain);
}

