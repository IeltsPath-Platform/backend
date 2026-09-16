package com.group01.user.infrastructure.adapter;

import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.exception.EmailAlreadyExistsException;
import com.group01.user.domain.exception.PhoneAlreadyExistsException;
import com.group01.user.domain.repository.UserRepository;
import com.group01.user.infrastructure.persistence.mapper.UserMapper;
import com.group01.user.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public List<User> findAll() {
        return userJpaRepository.findAll().stream().map(userMapper::toDomain).toList();
    }
}
