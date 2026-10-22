package com.ieltspath.user.infrastructure.persistence.repository;

import com.ieltspath.user.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    @Query(value = """
            SELECT u.id
            FROM users u
            JOIN user_roles ur ON ur.user_id = u.id
            JOIN roles r ON r.id = ur.role_id
            WHERE u.status = 'ACTIVE' AND r.name = 'ADMIN'
            ORDER BY u.id
            FOR UPDATE OF u
            """, nativeQuery = true)
    List<UUID> lockActiveAdminUserIds();
}
