package com.group01.content.application.usecase;

import com.group01.content.application.result.ReadingPassageResult;
import com.group01.content.domain.aggregate.ContentAsset;
import com.group01.content.domain.aggregate.ContentPackage;
import com.group01.content.domain.entity.ContentPackageVersion;
import com.group01.content.domain.entity.ContentSection;
import com.group01.content.domain.exception.ReadingPassageNotFoundException;
import com.group01.content.domain.repository.ContentAssetRepository;
import com.group01.content.domain.repository.ContentPackageRepository;
import com.group01.content.domain.vo.AssetType;
import com.group01.content.domain.vo.AssetValidationStatus;
import com.group01.content.domain.vo.PackageType;
import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.domain.vo.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetReadingPassageUseCaseTest {

    @Mock
    private ContentPackageRepository packages;
    @Mock
    private ContentAssetRepository assets;

    private GetReadingPassageUseCase useCase;
    private final UUID packageId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new GetReadingPassageUseCase(packages, assets);
    }

    private ContentPackage pkg(PackageType type, PublicationStatus status, UUID publishedVersionId, Skill skill) {
        ContentSection section = new ContentSection(sectionId, versionId, "Rooftops", skill, 1, 600,
                "Read the passage.", Instant.now(), Instant.now(), List.of());
        ContentPackageVersion version = new ContentPackageVersion(versionId, packageId, 1, PublicationStatus.PUBLISHED,
                "{}", 1, Instant.now(), null, Instant.now(), Instant.now(), List.of(section));
        return new ContentPackage(packageId, "CODE", "Practice set", type, null, status, publishedVersionId,
                Instant.now(), Instant.now(), List.of(version), null);
    }

    private static ContentAsset asset(AssetType type, String text) {
        return new ContentAsset(UUID.randomUUID(), type, text, null, null, null, AssetValidationStatus.VALID,
                Instant.now());
    }

    @Test
    void splitsPassagesIntoLabelledParagraphsAcrossAssetsInLinkOrder() {
        when(packages.findBySectionId(sectionId)).thenReturn(Optional.of(
                pkg(PackageType.PRACTICE_SET, PublicationStatus.PUBLISHED, versionId, Skill.READING)));
        when(assets.findBySectionId(sectionId)).thenReturn(List.of(
                asset(AssetType.PASSAGE, "First paragraph.\r\n\r\n  Second paragraph.  \n\n\n"),
                asset(AssetType.IMAGE, "ignored"),
                asset(AssetType.PASSAGE, "Third paragraph.")));

        ReadingPassageResult result = useCase.execute(sectionId);

        assertThat(result.sectionTitle()).isEqualTo("Rooftops");
        assertThat(result.instructions()).isEqualTo("Read the passage.");
        assertThat(result.packageId()).isEqualTo(packageId);
        assertThat(result.paragraphs()).containsExactly(
                new ReadingPassageResult.Paragraph("A", "First paragraph."),
                new ReadingPassageResult.Paragraph("B", "Second paragraph."),
                new ReadingPassageResult.Paragraph("C", "Third paragraph."));
    }

    @Test
    void lessonsAreReadable() {
        when(packages.findBySectionId(sectionId)).thenReturn(Optional.of(
                pkg(PackageType.LESSON, PublicationStatus.PUBLISHED, versionId, Skill.READING)));
        when(assets.findBySectionId(sectionId)).thenReturn(List.of(asset(AssetType.PASSAGE, "Text.")));

        assertThat(useCase.execute(sectionId).paragraphs()).hasSize(1);
    }

    @Test
    void mockAndPlacementTestsAreNotReadable() {
        for (PackageType type : List.of(PackageType.MOCK_TEST, PackageType.PLACEMENT_TEST, PackageType.QUIZ)) {
            when(packages.findBySectionId(sectionId)).thenReturn(Optional.of(
                    pkg(type, PublicationStatus.PUBLISHED, versionId, Skill.READING)));
            assertThrows(ReadingPassageNotFoundException.class, () -> useCase.execute(sectionId), type.name());
        }
    }

    @Test
    void unpublishedPackagesOldVersionsAndOtherSkillsAreNotReadable() {
        when(packages.findBySectionId(sectionId)).thenReturn(Optional.of(
                pkg(PackageType.PRACTICE_SET, PublicationStatus.DRAFT, versionId, Skill.READING)));
        assertThrows(ReadingPassageNotFoundException.class, () -> useCase.execute(sectionId));

        when(packages.findBySectionId(sectionId)).thenReturn(Optional.of(
                pkg(PackageType.PRACTICE_SET, PublicationStatus.PUBLISHED, UUID.randomUUID(), Skill.READING)));
        assertThrows(ReadingPassageNotFoundException.class, () -> useCase.execute(sectionId));

        when(packages.findBySectionId(sectionId)).thenReturn(Optional.of(
                pkg(PackageType.PRACTICE_SET, PublicationStatus.PUBLISHED, versionId, Skill.LISTENING)));
        assertThrows(ReadingPassageNotFoundException.class, () -> useCase.execute(sectionId));
    }

    @Test
    void unknownSectionsAndSectionsWithoutPassageTextAreNotReadable() {
        when(packages.findBySectionId(sectionId)).thenReturn(Optional.empty());
        assertThrows(ReadingPassageNotFoundException.class, () -> useCase.execute(sectionId));

        when(packages.findBySectionId(sectionId)).thenReturn(Optional.of(
                pkg(PackageType.PRACTICE_SET, PublicationStatus.PUBLISHED, versionId, Skill.READING)));
        when(assets.findBySectionId(sectionId)).thenReturn(List.of(
                asset(AssetType.PASSAGE, "  \n\n "), asset(AssetType.AUDIO, null)));
        assertThrows(ReadingPassageNotFoundException.class, () -> useCase.execute(sectionId));
    }

    @Test
    void passagesThatFailedValidationAreNotServed() {
        when(packages.findBySectionId(sectionId)).thenReturn(Optional.of(
                pkg(PackageType.PRACTICE_SET, PublicationStatus.PUBLISHED, versionId, Skill.READING)));
        ContentAsset invalid = new ContentAsset(UUID.randomUUID(), AssetType.PASSAGE, "Hidden.", null, null, null,
                AssetValidationStatus.INVALID, Instant.now());
        when(assets.findBySectionId(sectionId)).thenReturn(List.of(invalid, asset(AssetType.PASSAGE, "Shown.")));

        assertThat(useCase.execute(sectionId).paragraphs())
                .containsExactly(new ReadingPassageResult.Paragraph("A", "Shown."));
    }

    @Test
    void labelsContinuePastZWithNumbers() {
        assertThat(GetReadingPassageUseCase.label(0)).isEqualTo("A");
        assertThat(GetReadingPassageUseCase.label(25)).isEqualTo("Z");
        assertThat(GetReadingPassageUseCase.label(26)).isEqualTo("27");
    }
}
