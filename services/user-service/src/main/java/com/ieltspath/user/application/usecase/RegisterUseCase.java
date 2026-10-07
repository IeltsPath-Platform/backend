package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.CreateUserCommand;
import com.ieltspath.user.application.command.RegisterCommand;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.vo.RoleName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class RegisterUseCase {
    private final CreateUserUseCase createUserUseCase;

    @Transactional
    public User execute(RegisterCommand command) {
        String requestedRole = command.role() == null || command.role().isBlank()
                ? RoleName.CUSTOMER.name()
                : command.role();
        if (!RoleName.CUSTOMER.name().equals(requestedRole)) {
            throw new IllegalArgumentException("Public registration only supports the CUSTOMER role");
        }

        return createUserUseCase.execute(new CreateUserCommand(
                command.email(),
                command.password(),
                command.fullName(),
                command.phoneNumber(),
                Set.of(RoleName.CUSTOMER.name())
        ));
    }
}
