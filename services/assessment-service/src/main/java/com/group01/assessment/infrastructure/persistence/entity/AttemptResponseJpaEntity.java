package com.group01.assessment.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attempt_responses", uniqueConstraints = @UniqueConstraint(name = "uk_attempt_response_item", columnNames = "attempt_item_id"))
@Getter @Setter @NoArgsConstructor
public class AttemptResponseJpaEntity {
    @Id private UUID id;
    @Column(name = "attempt_item_id", nullable = false) private UUID attemptItemId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "response_payload", columnDefinition = "JSONB", nullable = false) private String payload;
    @Column(name = "schema_version", nullable = false) private int schemaVersion;
    @Column(nullable = false) private long revision;
    @Version @Column(name = "lock_version", nullable = false) private long lockVersion;
    @Column(name = "saved_at", nullable = false) private Instant savedAt;
    private Instant submittedAt;
}
