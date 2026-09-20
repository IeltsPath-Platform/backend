package com.group01.user.application.usecase;

import com.group01.user.application.command.CreateUserCommand;
import com.group01.user.application.command.RegisterCommand;
import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.vo.RoleName;
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
