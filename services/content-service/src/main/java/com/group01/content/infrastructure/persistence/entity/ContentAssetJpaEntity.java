package com.group01.content.infrastructure.persistence.entity;

import com.group01.content.domain.vo.AssetType;
import com.group01.content.domain.vo.AssetValidationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "content_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentAssetJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 50)
    private AssetType assetType;

    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;

    @Column(name = "media_reference", columnDefinition = "TEXT")
    private String mediaReference;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "checksum", length = 128)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 50)
    private AssetValidationStatus validationStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

