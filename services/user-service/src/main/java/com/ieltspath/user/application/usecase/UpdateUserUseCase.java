package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.UpdateUserCommand;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.PhoneAlreadyExistsException;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateUserUseCase {
    private final UserRepository userRepository;

    @Transactional
    public User execute(UpdateUserCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng: " + command.userId()));
        PhoneNumber phoneNumber = new PhoneNumber(command.phoneNumber());
        if (phoneNumber.value() != null && userRepository.existsByPhoneNumberAndIdNot(phoneNumber.value(), command.userId())) {
            throw new PhoneAlreadyExistsException("Số điện thoại đã tồn tại: " + phoneNumber.value());
        }
        user.updateProfile(command.fullName(), phoneNumber);
        return userRepository.save(user);
    }
}
