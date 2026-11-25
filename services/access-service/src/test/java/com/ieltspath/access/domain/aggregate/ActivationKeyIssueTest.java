package com.ieltspath.access.domain.aggregate;

import com.ieltspath.access.domain.exception.KeyNotIssuedException;
import com.ieltspath.access.domain.exception.KeyRevokedException;
import com.ieltspath.access.domain.vo.KeyStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivationKeyIssueTest {

    private static ActivationKey batchKey(Instant issuedAt) {
        return new ActivationKey(UUID.randomUUID(), UUID.randomUUID(), "hash-batch", "1234", KeyStatus.ACTIVE, null,
                null, Instant.now(), null, "B-2611-001", issuedAt);
    }

    @Test
    @DisplayName("A batch key that nobody has handed out cannot be redeemed, and keeps its state")
    void unissuedBatchKeyIsRefusedAndUnchanged() {
        ActivationKey key = batchKey(null);

        assertThatThrownBy(key::redeem).isInstanceOf(KeyNotIssuedException.class)
                .hasMessage("Mã kích hoạt không hợp lệ");
        assertThat(key.getStatus()).isEqualTo(KeyStatus.ACTIVE);
        assertThat(key.getRedeemedAt()).isNull();
    }

    @Test
    @DisplayName("A batch key handed out can be redeemed once")
    void issuedBatchKeyRedeems() {
        ActivationKey key = batchKey(Instant.now().minus(1, ChronoUnit.MINUTES));

        key.redeem();

        assertThat(key.getStatus()).isEqualTo(KeyStatus.REDEEMED);
        assertThat(key.getRedeemedAt()).isNotNull();
    }

    @Test
    @DisplayName("A revoked batch key that was never handed out says it is revoked, because only a revocation can make it so")
    void revokedUnissuedBatchKeyReportsRevocation() {
        ActivationKey key = batchKey(null);
        key.revoke();

        assertThatThrownBy(key::redeem).isInstanceOf(KeyRevokedException.class);
    }

    @Test
    @DisplayName("A key made one by one, with no batch, redeems without any handing-out step")
    void keyWithoutBatchNeedsNoIssue() {
        ActivationKey key = ActivationKey.create(UUID.randomUUID(), "hash-single", "9999", null, null);

        key.redeem();

        assertThat(key.getStatus()).isEqualTo(KeyStatus.REDEEMED);
    }
}
