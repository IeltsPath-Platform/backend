package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.AddContentSectionCommand;
import com.ieltspath.content.application.command.SaveContentSectionCommand;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.entity.ContentSection;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.exception.VersionNotDraftException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.PackageType;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Sections of a published version stay as learners see them, whichever route an author uses. */
class ContentSectionUseCasesTest {
    private final ContentPackageRepository packages = mock(ContentPackageRepository.class);
    private final ContentPackage pkg = ContentPackage.create("MOCK-LIVE", "Mock", PackageType.MOCK_TEST, null);
    private final ContentPackageVersion version = ContentPackageVersion.create(pkg.getId(), 1, "{}");
    private final ContentSection section = ContentSection.create(version.getId(), "Reading Passage 1", null, 1, null, null);

    ContentSectionUseCasesTest() {
        version.addSection(section);
        pkg.addVersion(version);
        version.publish(UUID.randomUUID());
        when(packages.findByIdForUpdate(pkg.getId())).thenReturn(Optional.of(pkg));
        when(packages.findByVersionId(version.getId())).thenReturn(Optional.of(pkg));
    }

    @Test
    void aSectionOfAPublishedVersionCannotBeUpdatedOrDeleted() {
        assertThatThrownBy(() -> new UpdateContentSectionUseCase(packages).execute(new SaveContentSectionCommand(
                pkg.getId(), version.getId(), section.getId(), "Renamed", null, 1, null, null, null, null)))
                .isInstanceOf(VersionNotDraftException.class);
        assertThatThrownBy(() -> new DeleteContentSectionUseCase(packages).execute(pkg.getId(), version.getId(), section.getId()))
                .isInstanceOf(VersionNotDraftException.class);
        assertThat(section.getTitle()).isEqualTo("Reading Passage 1");
        assertThat(version.getSections()).hasSize(1);
        verify(packages, never()).save(any());
    }

    @Test
    void theOldRouteCannotAddASectionToAPublishedVersion() {
        assertThatThrownBy(() -> new AddContentSectionUseCase(packages).execute(new AddContentSectionCommand(
                version.getId(), "Extra", null, 2, null, null))).isInstanceOf(VersionNotDraftException.class);
        assertThat(version.getSections()).hasSize(1);
        verify(packages, never()).save(any());
    }

    @Test
    void theOldRouteRefusesAnUnknownVersion() {
        UUID unknown = UUID.randomUUID();
        when(packages.findByVersionId(unknown)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new AddContentSectionUseCase(packages).execute(new AddContentSectionCommand(
                unknown, "Extra", null, 1, null, null))).isInstanceOf(ContentPackageNotFoundException.class);
        verify(packages, never()).save(any());
    }
}
