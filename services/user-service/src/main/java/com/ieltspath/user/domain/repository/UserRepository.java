package com.ieltspath.user.domain.repository;

import com.ieltspath.user.domain.aggregate.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    void clearPasswordHash(UUID userId);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id);
    List<UUID> lockActiveAdminUserIds();
    List<User> findAll();
    /** Up to {@code limit} users whose id sorts after {@code lastId}, in id order, for walking all accounts in batches. */
    List<User> findAfterId(UUID lastId, int limit);
}
