package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.EmailAlreadyExistsException;
import com.ieltspath.user.domain.exception.PhoneAlreadyExistsException;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.result.UserPage;
import com.ieltspath.user.domain.vo.RoleName;
import com.ieltspath.user.domain.vo.UserStatus;
import com.ieltspath.user.infrastructure.persistence.mapper.UserMapper;
import com.ieltspath.user.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {
    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;

    @Override
    public User save(User user) {
        try {
            return userMapper.toDomain(userJpaRepository.save(userMapper.toEntity(user)));
        } catch (DataIntegrityViolationException exception) {
            String detail = exception.getMostSpecificCause().getMessage();
            if (detail != null && detail.toLowerCase(java.util.Locale.ROOT).contains("email")) {
                throw new EmailAlreadyExistsException("Email already exists");
            }
            if (detail != null && detail.toLowerCase(java.util.Locale.ROOT).contains("phone")) {
                throw new PhoneAlreadyExistsException("Phone number already exists");
            }
            throw exception;
        }
    }

    @Override
    public void clearPasswordHash(UUID userId) {
        userJpaRepository.clearPasswordHash(userId);
    }

    @Override
    public int updatePasswordHashIfUnchanged(UUID userId, String oldHash, String newHash) {
        return userJpaRepository.updatePasswordHashIfUnchanged(userId, oldHash, newHash);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findWithRolesById(id).map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(userMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return userJpaRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id) {
        return userJpaRepository.existsByPhoneNumberAndIdNot(phoneNumber, id);
    }

    @Override
    public List<UUID> lockActiveAdminUserIds() {
        return userJpaRepository.lockActiveAdminUserIds();
    }

    @Override
    public List<User> findAll() {
        return userJpaRepository.findAll().stream().map(userMapper::toDomain).toList();
    }

    @Override
    public UserPage findPage(RoleName role, UserStatus status, String search, int page, int size) {
        Page<UUID> ids = userJpaRepository.findPageIds(role == null ? null : role.name(),
                status == null ? null : status.name(), search == null ? null : search.toLowerCase(Locale.ROOT),
                PageRequest.of(page, size, Sort.unsorted()));
        if (ids.isEmpty()) return new UserPage(List.of(), page, size, ids.getTotalElements());

        Map<UUID, User> users = userJpaRepository.findAllWithRolesByIdIn(ids.getContent()).stream()
                .map(userMapper::toDomain)
                .collect(Collectors.toMap(User::getId, Function.identity(), (first, ignored) -> first));
        List<User> ordered = ids.getContent().stream().map(users::get).filter(java.util.Objects::nonNull).toList();
        return new UserPage(ordered, page, size, ids.getTotalElements());
    }

    @Override
    public List<User> findAfterId(UUID lastId, int limit) {
        return userJpaRepository.findByIdGreaterThanOrderByIdAsc(lastId, org.springframework.data.domain.PageRequest.of(0, limit))
                .stream().map(userMapper::toDomain).toList();
    }
}
