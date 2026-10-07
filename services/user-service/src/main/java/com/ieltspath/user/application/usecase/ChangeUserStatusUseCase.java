package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.ChangeUserStatusCommand;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.InvalidUserStatusException;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangeUserStatusUseCase {
    private final UserRepository userRepository;

    @Transactional
    public User execute(ChangeUserStatusCommand command) {
        if (command.status() == null) {
            throw new InvalidUserStatusException("Trạng thái người dùng không được để trống");
        }
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng: " + command.userId()));
        user.changeStatus(command.status());
        return userRepository.save(user);
    }
}
