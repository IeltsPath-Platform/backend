package com.group01.content.infrastructure.persistence.entity;

import com.group01.content.domain.vo.AccessLevel;
import com.group01.content.domain.vo.PackageType;
import com.group01.content.domain.vo.PublicationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "content_packages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentPackageJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_type", nullable = false, length = 50)
    private PackageType packageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 50)
    private AccessLevel accessLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PublicationStatus status;

    @Column(name = "current_published_version_id")
    private UUID currentPublishedVersionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "packageId", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("versionNumber ASC")
    @Builder.Default
    private List<ContentPackageVersionJpaEntity> versions = new ArrayList<>();
}

