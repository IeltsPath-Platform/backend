package com.group01.user.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.user.api.dto.request.AssignRoleRequest;
import com.group01.user.api.dto.request.ChangeUserStatusRequest;
import com.group01.user.api.dto.request.CreateUserRequest;
import com.group01.user.api.dto.request.RegisterRequest;
import com.group01.user.api.dto.request.UpdateUserRequest;
import com.group01.user.api.dto.response.RoleResponse;
import com.group01.user.api.dto.response.UserResponse;
import com.group01.user.application.command.AssignRoleCommand;
import com.group01.user.application.command.ChangeUserStatusCommand;
import com.group01.user.application.command.CreateUserCommand;
import com.group01.user.application.command.RegisterCommand;
import com.group01.user.application.command.UpdateUserCommand;
import com.group01.user.application.usecase.AssignRoleUseCase;
import com.group01.user.application.usecase.ChangeUserStatusUseCase;
import com.group01.user.application.usecase.CreateUserUseCase;
import com.group01.user.application.usecase.DeleteUserUseCase;
import com.group01.user.application.usecase.GetAllUsersUseCase;
import com.group01.user.application.usecase.GetMyProfileUseCase;
import com.group01.user.application.usecase.GetUserByIdUseCase;
import com.group01.user.application.usecase.RegisterUseCase;
import com.group01.user.application.usecase.UpdateUserUseCase;
import com.group01.user.domain.aggregate.Role;
import com.group01.user.domain.aggregate.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final RegisterUseCase registerUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final GetMyProfileUseCase getMyProfileUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final GetAllUsersUseCase getAllUsersUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final AssignRoleUseCase assignRoleUseCase;
    private final ChangeUserStatusUseCase changeUserStatusUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return toResponse(createUserUseCase.execute(new CreateUserCommand(
                request.email(),
                request.password(),
                request.fullName(),
                request.phoneNumber(),
                request.roles()
        )));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #p0.toString() == authentication.name")
    public UserResponse getUserById(@PathVariable("id") UUID id) {
        return toResponse(getUserByIdUseCase.execute(id));
    }

    @GetMapping("/me")
    public UserResponse getMe() {
        return toResponse(getMyProfileUseCase.execute(currentUserProvider.requireUserId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAllUsers() {
        return getAllUsersUseCase.execute().stream().map(this::toResponse).toList();
    }

    @PutMapping("/me")
    public UserResponse updateMe(
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UUID userId = currentUserProvider.requireUserId();
        return toResponse(updateUserUseCase.execute(new UpdateUserCommand(
                userId,
                request.fullName(),
                request.phoneNumber()
        )));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #p0.toString() == authentication.name")
    public UserResponse updateUser(@PathVariable("id") UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return toResponse(updateUserUseCase.execute(new UpdateUserCommand(id, request.fullName(), request.phoneNumber())));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse assignRoles(@PathVariable("id") UUID id, @Valid @RequestBody AssignRoleRequest request) {
        return toResponse(assignRoleUseCase.execute(new AssignRoleCommand(id, request.roles())));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse changeStatus(@PathVariable("id") UUID id, @Valid @RequestBody ChangeUserStatusRequest request) {
        return toResponse(changeUserStatusUseCase.execute(new ChangeUserStatusCommand(id, request.status())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse deleteUser(@PathVariable("id") UUID id) {
        return toResponse(deleteUserUseCase.execute(id));
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
