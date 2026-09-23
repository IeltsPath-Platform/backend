package com.group01.access.domain.aggregate;

import com.group01.access.domain.exception.InvalidKeyOperationException;
import com.group01.access.domain.exception.KeyAlreadyRedeemedException;
import com.group01.access.domain.exception.KeyExpiredException;
import com.group01.access.domain.exception.KeyRevokedException;
import com.group01.access.domain.vo.KeyStatus;

import java.time.Instant;
import java.util.UUID;

public class ActivationKey {

    private final UUID id;
    private final UUID productId;
    private final String codeHash;
    private final String codeHint;
    private KeyStatus status;
    private final Instant expiresAt;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant redeemedAt;

    public ActivationKey(UUID id, UUID productId, String codeHash, String codeHint, KeyStatus status, Instant expiresAt, UUID createdBy, Instant createdAt, Instant redeemedAt) {
        this.id = id;
        this.productId = productId;
        this.codeHash = codeHash;
        this.codeHint = codeHint;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.redeemedAt = redeemedAt;
    }

    public static ActivationKey create(UUID productId, String codeHash, String codeHint, Instant expiresAt, UUID createdBy) {
        return new ActivationKey(UUID.randomUUID(), productId, codeHash, codeHint, KeyStatus.ACTIVE, expiresAt, createdBy, Instant.now(), null);
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public String getCodeHint() {
        return codeHint;
    }

    public KeyStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRedeemedAt() {
        return redeemedAt;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public boolean isRedeemable() {
        return status == KeyStatus.ACTIVE && !isExpired();
    }

    public void redeem() {
        if (status == KeyStatus.REDEEMED) {
            throw new KeyAlreadyRedeemedException("Activation key has already been redeemed");
        }
        if (status == KeyStatus.REVOKED) {
            throw new KeyRevokedException("Activation key has been revoked");
        }
        if (isExpired()) {
            this.status = KeyStatus.EXPIRED;
            throw new KeyExpiredException("Activation key has expired");
        }
        if (status != KeyStatus.ACTIVE) {
            throw new InvalidKeyOperationException("Activation key is not active: " + status);
        }

        this.status = KeyStatus.REDEEMED;
        this.redeemedAt = Instant.now();
    }

    public void revoke() {
        if (status == KeyStatus.REDEEMED) {
            throw new InvalidKeyOperationException("Cannot revoke a redeemed activation key");
        }
        this.status = KeyStatus.REVOKED;
    }
}
