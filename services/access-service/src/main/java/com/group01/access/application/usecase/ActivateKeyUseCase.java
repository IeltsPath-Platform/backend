package com.group01.access.application.usecase;

import com.group01.access.application.command.ActivateKeyCommand;
import com.group01.access.application.result.ActivationResult;
import com.group01.access.domain.aggregate.ActivationKey;
import com.group01.access.domain.aggregate.KeyProduct;
import com.group01.access.domain.aggregate.PointWallet;
import com.group01.access.domain.aggregate.Subscription;
import com.group01.access.domain.entity.KeyActivation;
import com.group01.access.domain.entity.OutboxEvent;
import com.group01.access.domain.entity.PointLedgerEntry;
import com.group01.access.domain.exception.ActivationKeyNotFoundException;
import com.group01.access.domain.exception.KeyProductNotFoundException;
import com.group01.access.domain.repository.*;
import com.group01.access.domain.vo.ActivationKeyCode;
import com.group01.access.domain.vo.KeyType;
import com.group01.access.domain.vo.PointTransactionType;
import com.group01.access.domain.vo.SubscriptionSourceType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ActivateKeyUseCase {

    private final ActivationKeyRepository activationKeyRepository;
    private final KeyProductRepository keyProductRepository;
    private final KeyActivationRepository keyActivationRepository;
    private final PointWalletRepository pointWalletRepository;
    private final PointLedgerRepository pointLedgerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final OutboxEventRepository outboxEventRepository;

    public ActivateKeyUseCase(
            ActivationKeyRepository activationKeyRepository,
            KeyProductRepository keyProductRepository,
            KeyActivationRepository keyActivationRepository,
            PointWalletRepository pointWalletRepository,
            PointLedgerRepository pointLedgerRepository,
            SubscriptionRepository subscriptionRepository,
            OutboxEventRepository outboxEventRepository
    ) {
        this.activationKeyRepository = activationKeyRepository;
        this.keyProductRepository = keyProductRepository;
        this.keyActivationRepository = keyActivationRepository;
        this.pointWalletRepository = pointWalletRepository;
        this.pointLedgerRepository = pointLedgerRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    public ActivationResult execute(ActivateKeyCommand command) {
        // Idempotency check
        Optional<KeyActivation> existingActivation = keyActivationRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existingActivation.isPresent()) {
            KeyActivation act = existingActivation.get();
            Long balance = act.getProductType() == KeyType.POINTS
                    ? pointWalletRepository.findByUserId(command.userId()).map(PointWallet::getBalance).orElse(0L)
                    : null;
            Instant endsAt = act.getProductType() == KeyType.PREMIUM
                    ? subscriptionRepository.findActiveByUserId(command.userId()).map(Subscription::getEndsAt).orElse(null)
                    : null;
            return new ActivationResult(
                    act.getId(), act.getKeyId(), act.getUserId(), act.getProductType(),
                    act.getPointsGranted(), act.getPremiumDaysGranted(), act.getHumanGradingCreditsGranted(),
                    act.getActivatedAt(), balance, endsAt
            );
        }

        ActivationKeyCode keyCode = ActivationKeyCode.fromRawKey(command.rawKey());
        ActivationKey key = activationKeyRepository.findByCodeHash(keyCode.codeHash())
                .orElseThrow(() -> new ActivationKeyNotFoundException("Mã kích hoạt không hợp lệ", true));

        key.redeem();

        KeyProduct product = keyProductRepository.findById(key.getProductId())
                .orElseThrow(() -> new KeyProductNotFoundException(key.getProductId()));

        UUID activationId = UUID.randomUUID();
        Long newBalance = null;
        Instant subEndsAt = null;

        if (product.isPointsProduct()) {
            int points = product.getPointsAmount() != null ? product.getPointsAmount() : 0;
            PointWallet wallet = pointWalletRepository.findByUserId(command.userId())
                    .orElseGet(() -> PointWallet.create(command.userId()));
            wallet.credit(points);
            pointWalletRepository.save(wallet);
            newBalance = wallet.getBalance();

            PointLedgerEntry ledgerEntry = PointLedgerEntry.create(
                    command.userId(),
                    points,
                    newBalance,
                    PointTransactionType.KEY_CREDIT,
                    "KEY_ACTIVATION",
                    activationId,
                    command.idempotencyKey(),
                    "Kích hoạt mã nạp point: " + product.getName()
            );
            pointLedgerRepository.save(ledgerEntry);

            KeyActivation keyActivation = new KeyActivation(
                    activationId, key.getId(), command.userId(), KeyType.POINTS,
                    points, 0, 0, Instant.now(), command.idempotencyKey()
            );
            keyActivationRepository.save(keyActivation);

            outboxEventRepository.save(OutboxEvent.create(
                    "PointWallet", command.userId(), "PointCredited",
                    String.format("{\"userId\":\"%s\",\"points\":%d,\"balance\":%d}", command.userId(), points, newBalance)
            ));
        } else {
            int days = product.getPremiumDays() != null ? product.getPremiumDays() : 0;
            int credits = product.getHumanGradingCredits() != null ? product.getHumanGradingCredits() : 0;

            Optional<Subscription> activeSub = subscriptionRepository.findActiveByUserId(command.userId());
            Subscription subscription;
            if (activeSub.isPresent()) {
                subscription = activeSub.get();
                subscription.extend(days, credits);
            } else {
                subscription = Subscription.create(
                        command.userId(), product.getPlanId(), SubscriptionSourceType.ACTIVATION_KEY,
                        activationId, days, credits
                );
            }
            subscriptionRepository.save(subscription);
            subEndsAt = subscription.getEndsAt();

            KeyActivation keyActivation = new KeyActivation(
                    activationId, key.getId(), command.userId(), KeyType.PREMIUM,
                    0, days, credits, Instant.now(), command.idempotencyKey()
            );
            keyActivationRepository.save(keyActivation);

            outboxEventRepository.save(OutboxEvent.create(
                    "Subscription", subscription.getId(), "SubscriptionChanged",
                    String.format("{\"userId\":\"%s\",\"subscriptionId\":\"%s\",\"endsAt\":\"%s\",\"credits\":%d}",
                            command.userId(), subscription.getId(), subEndsAt, subscription.getRemainingCredits())
            ));
        }

        activationKeyRepository.save(key);

        return new ActivationResult(
                activationId, key.getId(), command.userId(), product.getKeyType(),
                product.getPointsAmount() != null ? product.getPointsAmount() : 0,
                product.getPremiumDays() != null ? product.getPremiumDays() : 0,
                product.getHumanGradingCredits() != null ? product.getHumanGradingCredits() : 0,
                Instant.now(), newBalance, subEndsAt
        );
    }
}
