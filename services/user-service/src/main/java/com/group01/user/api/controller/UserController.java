package com.group01.user.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.user.api.dto.request.RegisterRequest;
import com.group01.user.api.dto.request.UpdateUserRequest;
import com.group01.user.api.dto.response.RoleResponse;
import com.group01.user.api.dto.response.UserResponse;
import com.group01.user.application.command.RegisterCommand;
import com.group01.user.application.command.UpdateUserCommand;
import com.group01.user.application.usecase.GetMyProfileUseCase;
import com.group01.user.application.usecase.RegisterUseCase;
import com.group01.user.application.usecase.UpdateUserUseCase;
import com.group01.user.domain.aggregate.Role;
import com.group01.user.domain.aggregate.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final RegisterUseCase registerUseCase;
    private final GetMyProfileUseCase getMyProfileUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return toResponse(registerUseCase.execute(new RegisterCommand(
                request.email(),
                request.password(),
                request.fullName(),
                request.phoneNumber(),
                request.role()
        )));
    }

    @GetMapping("/me")
    public UserResponse getMe() {
        return toResponse(getMyProfileUseCase.execute(currentUserProvider.requireUserId()));
    }

    @PutMapping("/me")
    public UserResponse updateMe(@Valid @RequestBody UpdateUserRequest request) {
        UUID userId = currentUserProvider.requireUserId();
        return toResponse(updateUserUseCase.execute(new UpdateUserCommand(
                userId,
                request.fullName(),
                request.phoneNumber()
        )));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail().value(),
                user.getFullName(),
                user.getPhoneNumber() == null ? null : user.getPhoneNumber().value(),
                user.getStatus().name(),
                toRoleResponses(user.getRoles()),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private Set<RoleResponse> toRoleResponses(Set<Role> roles) {
        return roles.stream()
                .map(role -> new RoleResponse(role.getId(), role.getName().name(), role.getDescription(), role.getCreatedAt(), role.getUpdatedAt()))
                .collect(Collectors.toSet());
    }
}
