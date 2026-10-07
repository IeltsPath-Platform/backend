package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUserByIdUseCase {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User execute(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng: " + id));
    }
}
