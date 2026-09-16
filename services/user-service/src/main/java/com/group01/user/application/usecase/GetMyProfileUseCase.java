package com.group01.user.application.usecase;

import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.exception.UserNotFoundException;
import com.group01.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetMyProfileUseCase {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User execute(UUID userId) {
        log.info("Get my profile requested userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy hồ sơ người dùng: " + userId));
        log.info("Get my profile completed userId={}", user.getId());
        return user;
    }
}
