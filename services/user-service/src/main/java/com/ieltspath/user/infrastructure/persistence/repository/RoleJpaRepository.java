package com.ieltspath.user.infrastructure.persistence.repository;

import com.ieltspath.user.infrastructure.persistence.entity.RoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, UUID> {
    Optional<RoleJpaEntity> findByName(String name);
    List<RoleJpaEntity> findByNameIn(Collection<String> names);
}
