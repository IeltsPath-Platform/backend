package com.ieltspath.notification.api;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.notification.application.PushDeviceUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** The signed-in user's phones that receive FCM push. The token is write-only: no endpoint returns it. */
@RestController
@RequestMapping("/api/notifications/devices")
public class PushDeviceController {
    private static final String INSTALLATION_ID = "[A-Za-z0-9_.:-]{8,200}";
    private final PushDeviceUseCase devices;
    private final CurrentUserProvider users;

    public PushDeviceController(PushDeviceUseCase devices, CurrentUserProvider users) {
        this.devices = devices;
        this.users = users;
    }

    @PutMapping("/{installationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@PathVariable("installationId") String installationId, @Valid @RequestBody RegisterRequest request) {
        devices.register(users.requireCurrentUser().id(), validInstallationId(installationId), request.platform(),
                request.pushToken());
    }

    @DeleteMapping("/{installationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable("installationId") String installationId) {
        if (!devices.revoke(users.requireCurrentUser().id(), validInstallationId(installationId)))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found");
    }

    private static String validInstallationId(String installationId) {
        if (!installationId.matches(INSTALLATION_ID))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid installation id");
        return installationId;
    }

    public record RegisterRequest(@NotBlank @Pattern(regexp = "ANDROID|IOS") String platform,
                                  @NotBlank @Size(max = 512) String pushToken) { }
}
