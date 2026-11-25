package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.user.api.dto.request.InitialPasswordRequest;
import com.ieltspath.user.api.dto.response.MessageResponse;
import com.ieltspath.user.application.usecase.SetInitialPasswordUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/password/initial")
@RequiredArgsConstructor
public class InitialPasswordController {
    private final SetInitialPasswordUseCase setInitialPassword;
    private final CurrentUserProvider currentUser;

    @PostMapping
    public MessageResponse set(@Valid @RequestBody InitialPasswordRequest request,
                               @RequestHeader(name = "X-Reauth-Token", required = false) String reauthToken) {
        setInitialPassword.execute(currentUser.requireUserId(), currentUser.currentSessionId().orElse(null),
                reauthToken, request.newPassword());
        return new MessageResponse("Đặt mật khẩu thành công. Vui lòng đăng nhập lại.");
    }
}
