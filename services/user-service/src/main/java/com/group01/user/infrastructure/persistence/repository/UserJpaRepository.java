package com.group01.user.infrastructure.persistence.repository;

import com.group01.user.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {
    @EntityGraph(attributePaths = "roles")
    Optional<UserJpaEntity> findWithRolesById(UUID id);

    @EntityGraph(attributePaths = "roles")
    Optional<UserJpaEntity> findByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    List<UserJpaEntity> findAll();

    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id);
}
