package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.repository.UserRepository;
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
