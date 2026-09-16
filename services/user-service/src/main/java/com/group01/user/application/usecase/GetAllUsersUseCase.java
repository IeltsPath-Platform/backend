package com.group01.user.application.usecase;

import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAllUsersUseCase {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<User> execute() {
        return userRepository.findAll();
    }
}
