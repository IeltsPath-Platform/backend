package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.UserRepository;
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
