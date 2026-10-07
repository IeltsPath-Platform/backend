package com.ieltspath.user.infrastructure.persistence.mapper;

import com.ieltspath.user.domain.aggregate.Role;
import com.ieltspath.user.domain.vo.RoleName;
import com.ieltspath.user.infrastructure.persistence.entity.RoleJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "name", expression = "java(toRoleName(entity.getName()))")
    Role toDomain(RoleJpaEntity entity);

    @Mapping(target = "name", expression = "java(role.getName().name())")
    RoleJpaEntity toEntity(Role role);

    default RoleName toRoleName(String name) {
        return RoleName.from(name);
    }
}
