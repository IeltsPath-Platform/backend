package com.group01.user.application.usecase;

import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.exception.UserNotFoundException;
import com.group01.user.domain.repository.UserRepository;
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
