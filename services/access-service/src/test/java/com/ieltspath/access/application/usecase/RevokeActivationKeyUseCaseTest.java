package com.ieltspath.access.application.usecase;

import com.ieltspath.access.domain.aggregate.ActivationKey;
import com.ieltspath.access.domain.exception.ActivationKeyNotFoundException;
import com.ieltspath.access.domain.exception.InvalidKeyOperationException;
import com.ieltspath.access.domain.repository.ActivationKeyRepository;
import com.ieltspath.access.domain.vo.KeyStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RevokeActivationKeyUseCaseTest {
    private final ActivationKeyRepository keys = mock(ActivationKeyRepository.class);
    private final RevokeActivationKeyUseCase useCase = new RevokeActivationKeyUseCase(keys);

    private ActivationKey stored() {
        ActivationKey key = ActivationKey.create(UUID.randomUUID(), "hash", "9999", null, null);
        when(keys.findById(key.getId())).thenReturn(Optional.of(key));
        return key;
    }

    @Test
    void anActiveKeyIsRevoked() {
        ActivationKey key = stored();

        useCase.execute(key.getId());

        assertThat(key.getStatus()).isEqualTo(KeyStatus.REVOKED);
        verify(keys).save(key);
    }

    @Test
    void aKeyThatDoesNotExistIsNotFound() {
        UUID missing = UUID.randomUUID();
        when(keys.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(missing)).isInstanceOf(ActivationKeyNotFoundException.class);
        verify(keys, never()).save(any());
    }

    @Test
    void aRedeemedKeyCannotBeRevoked() {
        ActivationKey key = stored();
        key.redeem();

        assertThatThrownBy(() -> useCase.execute(key.getId()))
                .isInstanceOf(InvalidKeyOperationException.class)
                .hasMessage("Cannot revoke a redeemed activation key");
        assertThat(key.getStatus()).isEqualTo(KeyStatus.REDEEMED);
        verify(keys, never()).save(any());
    }
}
