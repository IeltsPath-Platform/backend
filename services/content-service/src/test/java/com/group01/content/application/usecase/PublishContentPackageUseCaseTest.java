package com.group01.content.application.usecase;

import com.group01.content.application.command.PublishContentPackageCommand;
import com.group01.content.application.result.ContentPackageResult;
import com.group01.content.domain.aggregate.ContentPackage;
import com.group01.content.domain.entity.ContentPackageVersion;
import com.group01.content.domain.exception.ContentPackageNotFoundException;
import com.group01.content.domain.exception.InvalidContentStateException;
import com.group01.content.domain.repository.ContentPackageRepository;
import com.group01.content.domain.vo.AccessLevel;
import com.group01.content.domain.vo.PackageType;
import com.group01.content.domain.vo.PublicationStatus;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishContentPackageUseCaseTest {

    @Mock
    private ContentPackageRepository contentPackageRepository;

    @InjectMocks
    private PublishContentPackageUseCase publishContentPackageUseCase;

    private ContentPackage contentPackage;
    private ContentPackageVersion version;
    private UUID packageId;
    private UUID versionId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        packageId = UUID.randomUUID();
        userId = UUID.randomUUID();
        contentPackage = ContentPackage.create("PKG_01", "Mock Test", PackageType.MOCK_TEST, AccessLevel.FREE);
        version = ContentPackageVersion.create(contentPackage.getId(), 1, "{}");
        versionId = version.getId();
        contentPackage.addVersion(version);
    }

    @Test
    @DisplayName("Should publish content package version successfully")
    void shouldPublishPackageVersionSuccessfully() {
        when(contentPackageRepository.findById(contentPackage.getId())).thenReturn(Optional.of(contentPackage));
        when(contentPackageRepository.save(any(ContentPackage.class))).thenAnswer(inv -> inv.getArgument(0));

        PublishContentPackageCommand command = new PublishContentPackageCommand(contentPackage.getId(), versionId, userId);
        ContentPackageResult result = publishContentPackageUseCase.execute(command);

        assertThat(result.status()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(result.currentPublishedVersionId()).isEqualTo(versionId);
        verify(contentPackageRepository).save(contentPackage);
    }

    @Test
    @DisplayName("Should throw exception if package not found")
    void shouldThrowWhenPackageNotFound() {
        UUID nonExistent = UUID.randomUUID();
        when(contentPackageRepository.findById(nonExistent)).thenReturn(Optional.empty());

        PublishContentPackageCommand command = new PublishContentPackageCommand(nonExistent, versionId, userId);

        assertThatThrownBy(() -> publishContentPackageUseCase.execute(command))
                .isInstanceOf(ContentPackageNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception if version does not belong to package")
    void shouldThrowWhenVersionDoesNotBelongToPackage() {
        UUID wrongVersionId = UUID.randomUUID();
        when(contentPackageRepository.findById(contentPackage.getId())).thenReturn(Optional.of(contentPackage));

        PublishContentPackageCommand command = new PublishContentPackageCommand(contentPackage.getId(), wrongVersionId, userId);

        assertThatThrownBy(() -> publishContentPackageUseCase.execute(command))
                .isInstanceOf(InvalidContentStateException.class);
    }
}

