package com.group01.user.api.controller;

import com.group01.user.api.dto.request.AssignRoleRequest;
import com.group01.user.api.dto.request.ChangeLearningGoalStatusRequest;
import com.group01.user.api.dto.request.ChangeUserStatusRequest;
import com.group01.user.api.dto.request.CreateUserRequest;
import com.group01.user.api.dto.request.UpdateLearnerProfileRequest;
import com.group01.user.api.dto.request.UpdateUserRequest;
import com.group01.user.api.dto.response.ActionTokenResponse;
import com.group01.user.api.dto.response.LearnerProfileResponse;
import com.group01.user.api.dto.response.LearningGoalResponse;
import com.group01.user.api.dto.response.MessageResponse;
import com.group01.user.api.dto.response.OAuthIdentityResponse;
import com.group01.user.api.dto.response.RoleResponse;
import com.group01.user.api.dto.response.UserResponse;
import com.group01.user.application.command.AssignRoleCommand;
import com.group01.user.application.command.ChangeLearningGoalStatusCommand;
import com.group01.user.application.command.ChangeUserStatusCommand;
import com.group01.user.application.command.CreateUserCommand;
import com.group01.user.application.command.UpdateLearnerProfileCommand;
import com.group01.user.application.command.UpdateUserCommand;
import com.group01.user.application.result.AccountActionTokenResult;
import com.group01.user.application.result.LearnerProfileResult;
import com.group01.user.application.result.LearningGoalResult;
import com.group01.user.application.result.OAuthIdentityResult;
import com.group01.user.application.usecase.AssignRoleUseCase;
import com.group01.user.application.usecase.ChangeLearningGoalStatusUseCase;
import com.group01.user.application.usecase.ChangeUserStatusUseCase;
import com.group01.user.application.usecase.CreateUserUseCase;
import com.group01.user.application.usecase.DeleteLearnerProfileUseCase;
import com.group01.user.application.usecase.DeleteUserUseCase;
import com.group01.user.application.usecase.GetActionTokensByUserIdUseCase;
import com.group01.user.application.usecase.GetAllUsersUseCase;
import com.group01.user.application.usecase.GetLearnerProfileUseCase;
import com.group01.user.application.usecase.GetLearningGoalsByUserIdUseCase;
import com.group01.user.application.usecase.GetOAuthIdentitiesUseCase;
import com.group01.user.application.usecase.GetUserByIdUseCase;
import com.group01.user.application.usecase.RevokeUserActionTokensUseCase;
import com.group01.user.application.usecase.UnlinkOAuthIdentityUseCase;
import com.group01.user.application.usecase.UpdateLearnerProfileUseCase;
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
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final GetAllUsersUseCase getAllUsersUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final AssignRoleUseCase assignRoleUseCase;
    private final ChangeUserStatusUseCase changeUserStatusUseCase;
    private final DeleteUserUseCase deleteUserUseCase;

    private final GetLearnerProfileUseCase getLearnerProfileUseCase;
    private final UpdateLearnerProfileUseCase updateLearnerProfileUseCase;
    private final DeleteLearnerProfileUseCase deleteLearnerProfileUseCase;

    private final GetLearningGoalsByUserIdUseCase getLearningGoalsByUserIdUseCase;
    private final ChangeLearningGoalStatusUseCase changeLearningGoalStatusUseCase;

    private final GetOAuthIdentitiesUseCase getOAuthIdentitiesUseCase;
    private final UnlinkOAuthIdentityUseCase unlinkOAuthIdentityUseCase;

    private final GetActionTokensByUserIdUseCase getActionTokensByUserIdUseCase;
    private final RevokeUserActionTokensUseCase revokeUserActionTokensUseCase;

    // --- User Management ---
    @GetMapping
    public List<UserResponse> getAllUsers() {
        return getAllUsersUseCase.execute().stream().map(this::toUserResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return toUserResponse(createUserUseCase.execute(new CreateUserCommand(
                request.email(),
                request.password(),
                request.fullName(),
                request.phoneNumber(),
                request.roles()
        )));
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable("id") UUID id) {
        return toUserResponse(getUserByIdUseCase.execute(id));
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable("id") UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return toUserResponse(updateUserUseCase.execute(new UpdateUserCommand(id, request.fullName(), request.phoneNumber())));
    }

    @PutMapping("/{id}/roles")
    public UserResponse assignRoles(@PathVariable("id") UUID id, @Valid @RequestBody AssignRoleRequest request) {
        return toUserResponse(assignRoleUseCase.execute(new AssignRoleCommand(id, request.roles())));
    }

    @PutMapping("/{id}/status")
    public UserResponse changeStatus(@PathVariable("id") UUID id, @Valid @RequestBody ChangeUserStatusRequest request) {
        return toUserResponse(changeUserStatusUseCase.execute(new ChangeUserStatusCommand(id, request.status())));
    }

    @DeleteMapping("/{id}")
    public UserResponse deleteUser(@PathVariable("id") UUID id) {
        return toUserResponse(deleteUserUseCase.execute(id));
    }

    // --- Learner Profile Management ---
    @GetMapping("/{id}/profile")
    public LearnerProfileResponse getProfileById(@PathVariable("id") UUID id) {
        return toLearnerProfileResponse(getLearnerProfileUseCase.execute(id));
    }

    @PutMapping("/{id}/profile")
    public LearnerProfileResponse updateProfileById(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateLearnerProfileRequest request
    ) {
        return toLearnerProfileResponse(updateLearnerProfileUseCase.execute(new UpdateLearnerProfileCommand(
                id,
                request.displayName(),
                request.avatarReference(),
                request.bio(),
                request.selfReportedBand(),
                request.timezone(),
                request.visibility()
        )));
    }

    @DeleteMapping("/{id}/profile")
    public MessageResponse deleteProfileById(@PathVariable("id") UUID id) {
        deleteLearnerProfileUseCase.execute(id);
        return new MessageResponse("Xóa hồ sơ học tập thành công");
    }

    // --- Learning Goal Management ---
    @GetMapping("/{id}/learning-goals")
    public List<LearningGoalResponse> getLearningGoalsByUserId(@PathVariable("id") UUID id) {
        return getLearningGoalsByUserIdUseCase.execute(id).stream()
                .map(this::toLearningGoalResponse)
                .toList();
    }

    @PutMapping("/{id}/learning-goals/{goalId}/status")
    public LearningGoalResponse updateGoalStatusByAdmin(
            @PathVariable("id") UUID id,
            @PathVariable("goalId") UUID goalId,
            @Valid @RequestBody ChangeLearningGoalStatusRequest request
    ) {
        return toLearningGoalResponse(changeLearningGoalStatusUseCase.execute(new ChangeLearningGoalStatusCommand(
                goalId,
                null,
                request.status()
        )));
    }

    // --- OAuth Identity Management ---
    @GetMapping("/{id}/oauth")
    public List<OAuthIdentityResponse> getOAuthIdentitiesByUserId(@PathVariable("id") UUID id) {
        return getOAuthIdentitiesUseCase.execute(id).stream()
                .map(this::toOAuthIdentityResponse)
                .toList();
    }

    @DeleteMapping("/{id}/oauth/{provider}")
    public MessageResponse unlinkOAuthIdentityByAdmin(
            @PathVariable("id") UUID id,
            @PathVariable("provider") String provider
    ) {
        unlinkOAuthIdentityUseCase.execute(id, provider);
        return new MessageResponse("Hủy liên kết tài khoản OAuth của người dùng thành công");
    }

    // --- Action Token Management ---
    @GetMapping("/{id}/action-tokens")
    public List<ActionTokenResponse> getActionTokensByUserId(@PathVariable("id") UUID id) {
        return getActionTokensByUserIdUseCase.execute(id).stream()
                .map(this::toActionTokenResponse)
                .toList();
    }

    @DeleteMapping("/{id}/action-tokens")
    public MessageResponse revokeActionTokensByUserId(@PathVariable("id") UUID id) {
        revokeUserActionTokensUseCase.execute(id);
        return new MessageResponse("Thu hồi toàn bộ token của người dùng thành công");
    }

    // --- Mapping Helpers ---
    private UserResponse toUserResponse(User user) {
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

    private LearnerProfileResponse toLearnerProfileResponse(LearnerProfileResult result) {
        return new LearnerProfileResponse(
                result.userId(),
                result.displayName(),
                result.avatarReference(),
                result.bio(),
                result.selfReportedBand(),
                result.timezone(),
                result.visibility(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    private LearningGoalResponse toLearningGoalResponse(LearningGoalResult result) {
        return new LearningGoalResponse(
                result.id(),
                result.userId(),
                result.targetBand(),
                result.examDate(),
                result.availableMinutesPerDay(),
                result.status(),
                result.startedAt(),
                result.endedAt(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    private OAuthIdentityResponse toOAuthIdentityResponse(OAuthIdentityResult result) {
        return new OAuthIdentityResponse(
                result.id(),
                result.userId(),
                result.provider(),
                result.providerSubject(),
                result.linkedAt(),
                result.lastAuthenticatedAt()
        );
    }

    private ActionTokenResponse toActionTokenResponse(AccountActionTokenResult result) {
        return new ActionTokenResponse(
                result.id(),
                result.userId(),
                result.purpose(),
                result.expiresAt(),
                result.usedAt(),
                result.createdAt()
        );
    }
}
