package com.ieltspath.notification.application;

import com.ieltspath.notification.domain.repository.PushDeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** Registers and revokes the signed-in user's phones for push. */
@Service
public class PushDeviceUseCase {
    private final PushDeviceRepository devices;

    public PushDeviceUseCase(PushDeviceRepository devices) { this.devices = devices; }

    /** Idempotent: the same installation registering again only refreshes its token and owner. */
    @Transactional
    public void register(UUID userId, String installationId, String platform, String token) {
        devices.register(userId, installationId, platform, token, Instant.now());
    }

    /** @return false when the installation does not exist or belongs to someone else */
    @Transactional
    public boolean revoke(UUID userId, String installationId) { return devices.revoke(userId, installationId); }
}
