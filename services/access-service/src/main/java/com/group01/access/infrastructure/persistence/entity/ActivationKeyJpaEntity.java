package com.group01.access.infrastructure.persistence.entity;

import com.group01.access.domain.vo.KeyStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activation_keys")
@Getter
@Setter
@NoArgsConstructor
public class ActivationKeyJpaEntity {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "code_hash", nullable = false, unique = true, length = 128)
    private String codeHash;

    @Column(name = "code_hint", length = 20)
    private String codeHint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private KeyStatus status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;
}
