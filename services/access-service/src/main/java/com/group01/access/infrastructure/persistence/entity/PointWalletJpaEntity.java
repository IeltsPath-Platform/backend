package com.group01.access.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "point_wallets")
@Getter
@Setter
@NoArgsConstructor
public class PointWalletJpaEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private long balance;

    @Column(name = "total_credited", nullable = false)
    private long totalCredited;

    @Column(name = "total_debited", nullable = false)
    private long totalDebited;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
