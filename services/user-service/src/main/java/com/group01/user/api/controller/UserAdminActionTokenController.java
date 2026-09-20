package com.group01.user.api.controller;

import com.group01.user.api.dto.response.ActionTokenResponse;
import com.group01.user.api.dto.response.MessageResponse;
import com.group01.user.application.result.AccountActionTokenResult;
import com.group01.user.application.usecase.GetActionTokensByUserIdUseCase;
import com.group01.user.application.usecase.RevokeUserActionTokensUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserAdminActionTokenController {
    private final GetActionTokensByUserIdUseCase getActionTokensByUserIdUseCase;
    private final RevokeUserActionTokensUseCase revokeUserActionTokensUseCase;

    @GetMapping("/{id}/action-tokens")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ActionTokenResponse> getActionTokensByUserId(@PathVariable("id") UUID id) {
        return getActionTokensByUserIdUseCase.execute(id).stream()
                .map(this::toResponse)
                .toList();
    }

    @DeleteMapping("/{id}/action-tokens")
    @PreAuthorize("hasRole('ADMIN')")
    public MessageResponse revokeActionTokensByUserId(@PathVariable("id") UUID id) {
        revokeUserActionTokensUseCase.execute(id);
        return new MessageResponse("Thu hồi toàn bộ token của người dùng thành công");
    }

    private ActionTokenResponse toResponse(AccountActionTokenResult result) {
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

