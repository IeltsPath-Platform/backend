package com.ieltspath.notification.api;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.notification.application.DisableNotificationDeviceUseCase;
import com.ieltspath.notification.application.RegisterNotificationDeviceUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications/devices")
public class NotificationDeviceController {
    private final CurrentUserProvider users;
    private final RegisterNotificationDeviceUseCase register;
    private final DisableNotificationDeviceUseCase disable;

    public NotificationDeviceController(CurrentUserProvider users, RegisterNotificationDeviceUseCase register,
                                        DisableNotificationDeviceUseCase disable) {
        this.users = users;
        this.register = register;
        this.disable = disable;
    }

    @PutMapping("/{installationId}")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@PathVariable UUID installationId, @Valid @RequestBody Registration request) {
        try {
            register.execute(users.requireCurrentUser().id(), installationId, request.provider(), request.platform(),
                    request.token(), request.appVersion());
        } catch (IllegalArgumentException invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid device registration");
        }
    }

    @DeleteMapping("/{installationId}")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable UUID installationId) {
        disable.execute(users.requireCurrentUser().id(), installationId);
    }

    public record Registration(@NotNull String provider, @NotNull String platform,
                               @NotBlank @Size(max = 4096) String token, @Size(max = 128) String appVersion) {
        @Override
        public String toString() {
            return "Registration[provider=" + provider + ", platform=" + platform + ", token=[REDACTED], appVersion=" + appVersion + "]";
        }
    }
}
