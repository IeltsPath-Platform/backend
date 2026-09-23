package com.group01.access.application.usecase;

import com.group01.access.application.command.ActivateKeyCommand;
import com.group01.access.application.result.ActivationResult;
import com.group01.access.domain.aggregate.ActivationKey;
import com.group01.access.domain.aggregate.KeyProduct;
import com.group01.access.domain.aggregate.PointWallet;
import com.group01.access.domain.aggregate.Subscription;
import com.group01.access.domain.entity.KeyActivation;
import com.group01.access.domain.exception.ActivationKeyNotFoundException;
import com.group01.access.domain.exception.KeyAlreadyRedeemedException;
import com.group01.access.domain.repository.*;
import com.group01.access.domain.vo.ActivationKeyCode;
import com.group01.access.domain.vo.KeyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivateKeyUseCaseTest {

    @Mock
    private ActivationKeyRepository activationKeyRepository;
    @Mock
    private KeyProductRepository keyProductRepository;
    @Mock
    private KeyActivationRepository keyActivationRepository;
    @Mock
    private PointWalletRepository pointWalletRepository;
    @Mock
    private PointLedgerRepository pointLedgerRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private ActivateKeyUseCase activateKeyUseCase;

    private UUID userId;
    private String rawKey;
    private ActivationKeyCode keyCode;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        rawKey = "IP-TEST-1234-ABCD-5678";
        keyCode = ActivationKeyCode.fromRawKey(rawKey);
    }

    @Test
    @DisplayName("Should successfully activate POINTS key and credit wallet")
    void shouldActivatePointsKeySuccessfully() {
        UUID productId = UUID.randomUUID();
        ActivationKey key = ActivationKey.create(productId, keyCode.codeHash(), keyCode.codeHint(), null, null);
        KeyProduct product = KeyProduct.createPointsProduct("POINT_50", "50 Points", 50);

        when(keyActivationRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(activationKeyRepository.findByCodeHash(keyCode.codeHash())).thenReturn(Optional.of(key));
        when(keyProductRepository.findById(productId)).thenReturn(Optional.of(product));
        when(pointWalletRepository.findByUserId(userId)).thenReturn(Optional.of(PointWallet.create(userId)));

        ActivationResult result = activateKeyUseCase.execute(new ActivateKeyCommand(userId, rawKey, "idem-1"));

        assertThat(result).isNotNull();
        assertThat(result.productType()).isEqualTo(KeyType.POINTS);
        assertThat(result.pointsGranted()).isEqualTo(50);
        assertThat(result.newBalance()).isEqualTo(50L);

        verify(pointWalletRepository).save(any(PointWallet.class));
        verify(pointLedgerRepository).save(any());
        verify(keyActivationRepository).save(any(KeyActivation.class));
        verify(activationKeyRepository).save(key);
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Should successfully activate PREMIUM key and create new subscription")
    void shouldActivatePremiumKeySuccessfully() {
        UUID productId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        ActivationKey key = ActivationKey.create(productId, keyCode.codeHash(), keyCode.codeHint(), null, null);
        KeyProduct product = KeyProduct.createPremiumProduct("PREMIUM_30D", "30 Days Premium", planId, 30, 4);

        when(keyActivationRepository.findByIdempotencyKey("idem-2")).thenReturn(Optional.empty());
        when(activationKeyRepository.findByCodeHash(keyCode.codeHash())).thenReturn(Optional.of(key));
        when(keyProductRepository.findById(productId)).thenReturn(Optional.of(product));
        when(subscriptionRepository.findActiveByUserId(userId)).thenReturn(Optional.empty());

        ActivationResult result = activateKeyUseCase.execute(new ActivateKeyCommand(userId, rawKey, "idem-2"));

        assertThat(result).isNotNull();
        assertThat(result.productType()).isEqualTo(KeyType.PREMIUM);
        assertThat(result.premiumDaysGranted()).isEqualTo(30);
        assertThat(result.humanGradingCreditsGranted()).isEqualTo(4);
        assertThat(result.subscriptionEndsAt()).isNotNull();

        verify(subscriptionRepository).save(any(Subscription.class));
        verify(keyActivationRepository).save(any(KeyActivation.class));
        verify(activationKeyRepository).save(key);
    }

    @Test
    @DisplayName("Should return existing result on duplicate idempotency key")
    void shouldReturnExistingResultOnDuplicateIdempotency() {
        KeyActivation existing = KeyActivation.create(UUID.randomUUID(), userId, KeyType.POINTS, 50, 0, 0, "idem-dup");
        when(keyActivationRepository.findByIdempotencyKey("idem-dup")).thenReturn(Optional.of(existing));
        when(pointWalletRepository.findByUserId(userId)).thenReturn(Optional.of(new PointWallet(userId, 50, 50, 0, 0, null)));

        ActivationResult result = activateKeyUseCase.execute(new ActivateKeyCommand(userId, rawKey, "idem-dup"));

        assertThat(result.activationId()).isEqualTo(existing.getId());
        assertThat(result.pointsGranted()).isEqualTo(50);
        verifyNoInteractions(activationKeyRepository);
    }

    @Test
    @DisplayName("Should throw ActivationKeyNotFoundException when key does not exist")
    void shouldThrowWhenKeyNotFound() {
        when(keyActivationRepository.findByIdempotencyKey("idem-3")).thenReturn(Optional.empty());
        when(activationKeyRepository.findByCodeHash(keyCode.codeHash())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activateKeyUseCase.execute(new ActivateKeyCommand(userId, rawKey, "idem-3")))
                .isInstanceOf(ActivationKeyNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw KeyAlreadyRedeemedException when key was already redeemed")
    void shouldThrowWhenKeyAlreadyRedeemed() {
        ActivationKey key = ActivationKey.create(UUID.randomUUID(), keyCode.codeHash(), keyCode.codeHint(), null, null);
        key.redeem(); // already redeemed

        when(keyActivationRepository.findByIdempotencyKey("idem-4")).thenReturn(Optional.empty());
        when(activationKeyRepository.findByCodeHash(keyCode.codeHash())).thenReturn(Optional.of(key));

        assertThatThrownBy(() -> activateKeyUseCase.execute(new ActivateKeyCommand(userId, rawKey, "idem-4")))
                .isInstanceOf(KeyAlreadyRedeemedException.class);
    }
}
