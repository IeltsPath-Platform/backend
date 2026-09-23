package com.group01.access.infrastructure.persistence.mapper;

import com.group01.access.domain.aggregate.*;
import com.group01.access.domain.entity.*;
import com.group01.access.infrastructure.persistence.entity.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class AccessPersistenceMapper {

    // ==========================================
    // PLAN & PLAN FEATURE
    // ==========================================
    public Plan toDomain(PlanJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        List<PlanFeature> features = entity.getFeatures() != null
                ? entity.getFeatures().stream().map(this::toDomain).toList()
                : Collections.emptyList();

        return new Plan(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getStatus(),
                features,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public PlanJpaEntity toEntity(Plan domain) {
        if (domain == null) {
            return null;
        }
        PlanJpaEntity entity = new PlanJpaEntity();
        entity.setId(domain.getId());
        entity.setCode(domain.getCode());
        entity.setName(domain.getName());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        if (domain.getFeatures() != null) {
            List<PlanFeatureJpaEntity> featureEntities = new ArrayList<>();
            for (PlanFeature f : domain.getFeatures()) {
                PlanFeatureJpaEntity fe = toEntity(f, entity);
                featureEntities.add(fe);
            }
            entity.setFeatures(featureEntities);
        }
        return entity;
    }

    public PlanFeature toDomain(PlanFeatureJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PlanFeature(
                entity.getId(),
                entity.getPlan() != null ? entity.getPlan().getId() : null,
                entity.getFeatureKey(),
                entity.isEnabled(),
                entity.getLimitValue(),
                entity.getConfig(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public PlanFeatureJpaEntity toEntity(PlanFeature domain, PlanJpaEntity planEntity) {
        if (domain == null) {
            return null;
        }
        PlanFeatureJpaEntity entity = new PlanFeatureJpaEntity();
        entity.setId(domain.getId());
        entity.setPlan(planEntity);
        entity.setFeatureKey(domain.getFeatureKey());
        entity.setEnabled(domain.isEnabled());
        entity.setLimitValue(domain.getLimitValue());
        entity.setConfig(domain.getConfig());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    // ==========================================
    // SUBSCRIPTION
    // ==========================================
    public Subscription toDomain(SubscriptionJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Subscription(
                entity.getId(),
                entity.getUserId(),
                entity.getPlanId(),
                entity.getStatus(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.getSourceType(),
                entity.getSourceReferenceId(),
                entity.getHumanGradingCreditsTotal(),
                entity.getHumanGradingCreditsUsed(),
                entity.getCancelledAt(),
                entity.getRowVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public SubscriptionJpaEntity toEntity(Subscription domain) {
        if (domain == null) {
            return null;
        }
        SubscriptionJpaEntity entity = new SubscriptionJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setPlanId(domain.getPlanId());
        entity.setStatus(domain.getStatus());
        entity.setStartsAt(domain.getStartsAt());
        entity.setEndsAt(domain.getEndsAt());
        entity.setSourceType(domain.getSourceType());
        entity.setSourceReferenceId(domain.getSourceReferenceId());
        entity.setHumanGradingCreditsTotal(domain.getHumanGradingCreditsTotal());
        entity.setHumanGradingCreditsUsed(domain.getHumanGradingCreditsUsed());
        entity.setCancelledAt(domain.getCancelledAt());
        entity.setRowVersion(domain.getRowVersion());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    // ==========================================
    // KEY PRODUCT
    // ==========================================
    public KeyProduct toDomain(KeyProductJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new KeyProduct(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getKeyType(),
                entity.getPointsAmount(),
                entity.getPlanId(),
                entity.getPremiumDays(),
                entity.getHumanGradingCredits(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public KeyProductJpaEntity toEntity(KeyProduct domain) {
        if (domain == null) {
            return null;
        }
        KeyProductJpaEntity entity = new KeyProductJpaEntity();
        entity.setId(domain.getId());
        entity.setCode(domain.getCode());
        entity.setName(domain.getName());
        entity.setKeyType(domain.getKeyType());
        entity.setPointsAmount(domain.getPointsAmount());
        entity.setPlanId(domain.getPlanId());
        entity.setPremiumDays(domain.getPremiumDays());
        entity.setHumanGradingCredits(domain.getHumanGradingCredits());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    // ==========================================
    // ACTIVATION KEY
    // ==========================================
    public ActivationKey toDomain(ActivationKeyJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ActivationKey(
                entity.getId(),
                entity.getProductId(),
                entity.getCodeHash(),
                entity.getCodeHint(),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getRedeemedAt()
        );
    }

    public ActivationKeyJpaEntity toEntity(ActivationKey domain) {
        if (domain == null) {
            return null;
        }
        ActivationKeyJpaEntity entity = new ActivationKeyJpaEntity();
        entity.setId(domain.getId());
        entity.setProductId(domain.getProductId());
        entity.setCodeHash(domain.getCodeHash());
        entity.setCodeHint(domain.getCodeHint());
        entity.setStatus(domain.getStatus());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setRedeemedAt(domain.getRedeemedAt());
        return entity;
    }

    // ==========================================
    // KEY ACTIVATION
    // ==========================================
    public KeyActivation toDomain(KeyActivationJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new KeyActivation(
                entity.getId(),
                entity.getKeyId(),
                entity.getUserId(),
                entity.getProductType(),
                entity.getPointsGranted(),
                entity.getPremiumDaysGranted(),
                entity.getHumanGradingCreditsGranted(),
                entity.getActivatedAt(),
                entity.getIdempotencyKey()
        );
    }

    public KeyActivationJpaEntity toEntity(KeyActivation domain) {
        if (domain == null) {
            return null;
        }
        KeyActivationJpaEntity entity = new KeyActivationJpaEntity();
        entity.setId(domain.getId());
        entity.setKeyId(domain.getKeyId());
        entity.setUserId(domain.getUserId());
        entity.setProductType(domain.getProductType());
        entity.setPointsGranted(domain.getPointsGranted());
        entity.setPremiumDaysGranted(domain.getPremiumDaysGranted());
        entity.setHumanGradingCreditsGranted(domain.getHumanGradingCreditsGranted());
        entity.setActivatedAt(domain.getActivatedAt());
        entity.setIdempotencyKey(domain.getIdempotencyKey());
        return entity;
    }

    // ==========================================
    // POINT WALLET
    // ==========================================
    public PointWallet toDomain(PointWalletJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PointWallet(
                entity.getUserId(),
                entity.getBalance(),
                entity.getTotalCredited(),
                entity.getTotalDebited(),
                entity.getRowVersion(),
                entity.getUpdatedAt()
        );
    }

    public PointWalletJpaEntity toEntity(PointWallet domain) {
        if (domain == null) {
            return null;
        }
        PointWalletJpaEntity entity = new PointWalletJpaEntity();
        entity.setUserId(domain.getUserId());
        entity.setBalance(domain.getBalance());
        entity.setTotalCredited(domain.getTotalCredited());
        entity.setTotalDebited(domain.getTotalDebited());
        entity.setRowVersion(domain.getRowVersion());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    // ==========================================
    // POINT LEDGER ENTRY
    // ==========================================
    public PointLedgerEntry toDomain(PointLedgerEntryJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PointLedgerEntry(
                entity.getId(),
                entity.getUserId(),
                entity.getDelta(),
                entity.getBalanceAfter(),
                entity.getTransactionType(),
                entity.getReferenceType(),
                entity.getReferenceId(),
                entity.getIdempotencyKey(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }

    public PointLedgerEntryJpaEntity toEntity(PointLedgerEntry domain) {
        if (domain == null) {
            return null;
        }
        PointLedgerEntryJpaEntity entity = new PointLedgerEntryJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setDelta(domain.getDelta());
        entity.setBalanceAfter(domain.getBalanceAfter());
        entity.setTransactionType(domain.getTransactionType());
        entity.setReferenceType(domain.getReferenceType());
        entity.setReferenceId(domain.getReferenceId());
        entity.setIdempotencyKey(domain.getIdempotencyKey());
        entity.setDescription(domain.getDescription());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    // ==========================================
    // OUTBOX EVENT
    // ==========================================
    public OutboxEvent toDomain(OutboxEventJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new OutboxEvent(
                entity.getId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getStatus(),
                entity.getRetryCount(),
                entity.getCreatedAt(),
                entity.getPublishedAt()
        );
    }

    public OutboxEventJpaEntity toEntity(OutboxEvent domain) {
        if (domain == null) {
            return null;
        }
        OutboxEventJpaEntity entity = new OutboxEventJpaEntity();
        entity.setId(domain.getId());
        entity.setAggregateType(domain.getAggregateType());
        entity.setAggregateId(domain.getAggregateId());
        entity.setEventType(domain.getEventType());
        entity.setPayload(domain.getPayload());
        entity.setStatus(domain.getStatus());
        entity.setRetryCount(domain.getRetryCount());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setPublishedAt(domain.getPublishedAt());
        return entity;
    }
}
