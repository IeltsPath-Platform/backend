package com.ieltspath.access.application.usecase;

import static org.mockito.ArgumentMatchers.anyString;
import java.time.temporal.ChronoUnit;
import java.time.Instant;
import org.mockito.ArgumentCaptor;
import com.ieltspath.access.domain.vo.SubscriptionStatus;
import com.ieltspath.access.domain.vo.SubscriptionSourceType;
import com.ieltspath.access.domain.exception.KeyRevokedException;
import com.ieltspath.access.domain.exception.KeyProductNotFoundException;
import com.ieltspath.access.domain.exception.KeyExpiredException;
import com.ieltspath.access.application.command.ActivateKeyCommand;
import com.ieltspath.access.application.result.ActivationResult;
import com.ieltspath.access.domain.aggregate.ActivationKey;
import com.ieltspath.access.domain.aggregate.KeyProduct;
import com.ieltspath.access.domain.aggregate.PointWallet;
import com.ieltspath.access.domain.aggregate.Subscription;
import com.ieltspath.access.domain.entity.KeyActivation;
import com.ieltspath.access.domain.exception.ActivationKeyNotFoundException;
import com.ieltspath.access.domain.exception.KeyAlreadyRedeemedException;
import com.ieltspath.access.domain.repository.*;
import com.ieltspath.access.domain.vo.ActivationKeyCode;
import com.ieltspath.access.domain.vo.KeyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.ieltspath.access.domain.repository.UserEmailVerificationRepository;
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

    @Mock
    UserEmailVerificationRepository userEmailVerificationRepository;

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
        org.mockito.Mockito.lenient().when(userEmailVerificationRepository.isVerified(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    }

    @Test
    @DisplayName("Should refuse a key for an account with no verified state, and never redeem it")
    void shouldRefuseKeyWhenAccountIsNotKnownToBeVerified() {
        when(userEmailVerificationRepository.isVerified(userId)).thenReturn(false);

        assertThatThrownBy(() -> activateKeyUseCase.execute(new ActivateKeyCommand(userId, rawKey, "idem-unverified")))
                .isInstanceOf(com.ieltspath.access.domain.exception.EmailNotVerifiedException.class);
        verifyNoInteractions(activationKeyRepository);
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
        ActivationKey key = ActivationKey.create(UUID.randomUUID(), keyCode.codeHash(), keyCode.codeHint(), null, null);
        KeyActivation existing = KeyActivation.create(key.getId(), userId, KeyType.POINTS, 50, 0, 0, "idem-dup");
        when(keyActivationRepository.findByIdempotencyKey("idem-dup")).thenReturn(Optional.of(existing));
        when(activationKeyRepository.findByCodeHash(keyCode.codeHash())).thenReturn(Optional.of(key));
        when(pointWalletRepository.findByUserId(userId)).thenReturn(Optional.of(new PointWallet(userId, 50, 50, 0, 0, null)));

        ActivationResult result = activateKeyUseCase.execute(new ActivateKeyCommand(userId, rawKey, "idem-dup"));

        assertThat(result.activationId()).isEqualTo(existing.getId());
        assertThat(result.pointsGranted()).isEqualTo(50);
        verify(activationKeyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject a replayed idempotency key from a different user")
    void shouldRejectReplayFromDifferentUser() {
        KeyActivation existing = KeyActivation.create(UUID.randomUUID(), UUID.randomUUID(), KeyType.POINTS, 50, 0, 0, "idem-other-user");
        when(keyActivationRepository.findByIdempotencyKey("idem-other-user")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> activateKeyUseCase.execute(new ActivateKeyCommand(userId, rawKey, "idem-other-user")))
                .isInstanceOf(com.ieltspath.access.domain.exception.DuplicateIdempotencyException.class);
        verifyNoInteractions(pointWalletRepository, subscriptionRepository);
    }

    @Test
    @DisplayName("Should reject a replayed idempotency key sent with a different activation key")
    void shouldRejectReplayWithDifferentKey() {
        KeyActivation existing = KeyActivation.create(UUID.randomUUID(), userId, KeyType.POINTS, 50, 0, 0, "idem-other-key");
        ActivationKey otherKey = ActivationKey.create(UUID.randomUUID(), keyCode.codeHash(), keyCode.codeHint(), null, null);
        when(keyActivationRepository.findByIdempotencyKey("idem-other-key")).thenReturn(Optional.of(existing));
        when(activationKeyRepository.findByCodeHash(keyCode.codeHash())).thenReturn(Optional.of(otherKey));

        assertThatThrownBy(() -> activateKeyUseCase.execute(new ActivateKeyCommand(userId, rawKey, "idem-other-key")))
                .isInstanceOf(com.ieltspath.access.domain.exception.DuplicateIdempotencyException.class);
        verifyNoInteractions(pointWalletRepository, subscriptionRepository);
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

    @Test
    @DisplayName("Expired and revoked keys grant nothing")
    void shouldRefuseExpiredAndRevokedKeys() {
        ActivationKey expired = ActivationKey.create(UUID.randomUUID(), keyCode.codeHash(), keyCode.codeHint(),
                Instant.now().minus(1, ChronoUnit.DAYS), null);
        when(keyActivationRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(activationKeyRepository.findByCodeHash(keyCode.codeHash())).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> activateKeyUseCase.execute(new ActivateKeyCommand(userId, rawKey, "idem-exp")))
                .isInstanceOf(KeyExpiredException.class);

        ActivationKey revoked = ActivationKey.create(UUID.randomUUID(), keyCode.codeHash(), keyCode.codeHint(), null, null);
        revoked.revoke();
        when(activationKeyRepository.findByCodeHash(keyCode.codeHash())).thenReturn(Optional.of(revoked));
        assertThatThrownBy(() -> activateKeyUseCase.execute(new ActivateKeyCommand(userId, rawKey, "idem-rev")))
                .isInstanceOf(KeyRevokedException.class);

        verifyNoInteractions(keyProductRepository, pointWalletRepository, subscriptionRepository, outboxEventRepository);
        verify(activationKeyRepository, never()).save(any());
    }

    @Test
    @DisplayName("A key whose product does not exist grants nothing")
    void shouldThrowWhenProductMissing() {
        UUID productId = UUID.randomUUID();
        ActivationKey key = ActivationKey.create(productId, keyCode.codeHash(), keyCode.codeHint(), null, null);
        when(keyActivationRepository.findByIdempotencyKey("idem-5")).thenReturn(Optional.empty());
        when(activationKeyRepository.findByCodeHash(keyCode.codeHash())).thenReturn(Optional.of(key));
        when(keyProductRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activateKeyUseCase.execute(new ActivateKeyCommand(userId, rawKey, "idem-5")))
                .isInstanceOf(KeyProductNotFoundException.class);
        verifyNoInteractions(pointWalletRepository, subscriptionRepository, outboxEventRepository);
        verify(activationKeyRepository, never()).save(any());
    }

    @Test
    @DisplayName("A premium key extends the active subscription")
    void shouldExtendActiveSubscriptionWithPremiumKey() {
        UUID productId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        Instant now = Instant.now();
        ActivationKey key = ActivationKey.create(productId, keyCode.codeHash(), keyCode.codeHint(), null, null);
        Subscription active = new Subscription(UUID.randomUUID(), userId, planId, SubscriptionStatus.ACTIVE, now.minus(20, ChronoUnit.DAYS),
                now.plus(10, ChronoUnit.DAYS), SubscriptionSourceType.ACTIVATION_KEY, UUID.randomUUID(), 2, 0, null, 0, null, null);
        when(keyActivationRepository.findByIdempotencyKey("idem-6")).thenReturn(Optional.empty());
        when(activationKeyRepository.findByCodeHash(keyCode.codeHash())).thenReturn(Optional.of(key));
        when(keyProductRepository.findById(productId)).thenReturn(Optional.of(KeyProduct.createPremiumProduct("PREMIUM_30D", "30 Days Premium", planId, 30, 4)));
        when(subscriptionRepository.findActiveByUserId(userId)).thenReturn(Optional.of(active));

        ActivationResult result = activateKeyUseCase.execute(new ActivateKeyCommand(userId, rawKey, "idem-6"));

        assertThat(active.getEndsAt()).isAfter(now.plus(39, ChronoUnit.DAYS)).isBefore(now.plus(41, ChronoUnit.DAYS));
        assertThat(active.getRemainingCredits()).isEqualTo(6);
        assertThat(result.subscriptionEndsAt()).isEqualTo(active.getEndsAt());
        verify(subscriptionRepository).save(active);
    }

    @Test
    @DisplayName("The first points key of a learner creates the wallet")
    void shouldCreateWalletForFirstPointsKey() {
        UUID productId = UUID.randomUUID();
        ActivationKey key = ActivationKey.create(productId, keyCode.codeHash(), keyCode.codeHint(), null, null);
        when(keyActivationRepository.findByIdempotencyKey("idem-7")).thenReturn(Optional.empty());
        when(activationKeyRepository.findByCodeHash(keyCode.codeHash())).thenReturn(Optional.of(key));
        when(keyProductRepository.findById(productId)).thenReturn(Optional.of(KeyProduct.createPointsProduct("POINT_50", "50 Points", 50)));
        when(pointWalletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        ActivationResult result = activateKeyUseCase.execute(new ActivateKeyCommand(userId, rawKey, "idem-7"));

        ArgumentCaptor<PointWallet> saved = ArgumentCaptor.forClass(PointWallet.class);
        verify(pointWalletRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);
        assertThat(saved.getValue().getBalance()).isEqualTo(50);
        assertThat(result.newBalance()).isEqualTo(50L);
    }
}
