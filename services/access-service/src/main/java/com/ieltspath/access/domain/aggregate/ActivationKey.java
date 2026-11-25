package com.ieltspath.access.domain.aggregate;

import com.ieltspath.access.domain.exception.InvalidKeyOperationException;
import com.ieltspath.access.domain.exception.KeyAlreadyRedeemedException;
import com.ieltspath.access.domain.exception.KeyExpiredException;
import com.ieltspath.access.domain.exception.KeyNotIssuedException;
import com.ieltspath.access.domain.exception.KeyRevokedException;
import com.ieltspath.access.domain.vo.KeyStatus;

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
    /** The batch the key was made in; empty for keys made one by one, which count as handed out from the start. */
    private final String batchCode;
    /** When a sale or manager handed the key to a student. A batch key cannot be redeemed before this is set. */
    private final Instant issuedAt;

    public ActivationKey(UUID id, UUID productId, String codeHash, String codeHint, KeyStatus status, Instant expiresAt, UUID createdBy, Instant createdAt, Instant redeemedAt) {
        this(id, productId, codeHash, codeHint, status, expiresAt, createdBy, createdAt, redeemedAt, null, null);
    }

    public ActivationKey(UUID id, UUID productId, String codeHash, String codeHint, KeyStatus status, Instant expiresAt,
                         UUID createdBy, Instant createdAt, Instant redeemedAt, String batchCode, Instant issuedAt) {
        this.batchCode = batchCode;
        this.issuedAt = issuedAt;
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

    public String getBatchCode() {
        return batchCode;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    /** A key of a batch is handed out first; a key without a batch has no such step. */
    public boolean isIssued() {
        return batchCode == null || issuedAt != null;
    }

    public boolean isRedeemable() {
        return status == KeyStatus.ACTIVE && !isExpired() && isIssued();
    }

    public void redeem() {
        if (status == KeyStatus.REDEEMED) {
            throw new KeyAlreadyRedeemedException("Mã này đã được sử dụng.");
        }
        if (status == KeyStatus.REVOKED) {
            throw new KeyRevokedException("Mã kích hoạt đã bị thu hồi.");
        }
        // A key nobody has handed out answers like a code that does not exist (see KeyNotIssuedException).
        if (!isIssued()) {
            throw new KeyNotIssuedException();
        }
        if (isExpired()) {
            this.status = KeyStatus.EXPIRED;
            throw new KeyExpiredException("Mã kích hoạt đã hết hạn.");
        }
        if (status != KeyStatus.ACTIVE) {
            throw new InvalidKeyOperationException("Mã kích hoạt không còn dùng được.");
        }

        this.status = KeyStatus.REDEEMED;
        this.redeemedAt = Instant.now();
    }

    public void revoke() {
        if (status == KeyStatus.REDEEMED) {
            throw new InvalidKeyOperationException("Không thu hồi được mã đã kích hoạt.");
        }
        this.status = KeyStatus.REVOKED;
    }
}
