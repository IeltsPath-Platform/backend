package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.entity.ContentSection;
import com.ieltspath.content.domain.exception.ContentSectionNotFoundException;
import com.ieltspath.content.domain.exception.DraftVersionExistsException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.exception.VersionNotDraftException;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContentPackageAuthoringTest {

    @Test
    void secondDraftIsRefusedWhileOneIsOpen() {
        ContentPackage pkg = newPackage();
        pkg.createDraftVersion("{}");

        assertThrows(DraftVersionExistsException.class, () -> pkg.createDraftVersion("{}"));
        assertEquals(1, pkg.getVersions().size());
    }

    @Test
    void newDraftNumbersAfterTheHighestVersion() {
        ContentPackage pkg = newPackage();
        pkg.addVersion(publishedVersion(pkg.getId(), 1));
        pkg.addVersion(publishedVersion(pkg.getId(), 3));

        ContentPackageVersion draft = pkg.createDraftVersion("{}");

        assertEquals(4, draft.getVersionNumber());
        assertEquals(PublicationStatus.DRAFT, draft.getStatus());
    }

    @Test
    void sectionOfAnotherVersionIsABadRequestNotNotFound() {
        ContentPackage pkg = newPackage();
        ContentPackageVersion draft = pkg.createDraftVersion("{}");
        ContentPackageVersion other = ContentPackageVersion.create(pkg.getId(), 1, "{}");
        ContentSection foreign = ContentSection.create(other.getId(), "Other", null, 1, null, null);
        other.addSection(foreign);
        other.publish(UUID.randomUUID());
        pkg.addVersion(other);

        assertThrows(IllegalArgumentException.class,
                () -> pkg.requireSectionInVersion(draft.getId(), foreign.getId()));
    }

    @Test
    void sectionThatExistsNowhereIsNotFound() {
        ContentPackage pkg = newPackage();
        ContentPackageVersion draft = pkg.createDraftVersion("{}");

        assertThrows(ContentSectionNotFoundException.class,
                () -> pkg.requireSectionInVersion(draft.getId(), UUID.randomUUID()));
    }

    @Test
    void reorderGivesContiguousOrdersInTheGivenSequence() {
        ContentPackage pkg = newPackage();
        ContentPackageVersion draft = pkg.createDraftVersion("{}");
        ContentSection a = section(draft, 1);
        ContentSection b = section(draft, 2);
        ContentSection c = section(draft, 3);

        draft.reorderSections(List.of(c.getId(), a.getId(), b.getId()));

        assertEquals(1, c.getSortOrder());
        assertEquals(2, a.getSortOrder());
        assertEquals(3, b.getSortOrder());
    }

    @Test
    void reorderRefusesAnIncompleteOrDuplicatedList() {
        ContentPackage pkg = newPackage();
        ContentPackageVersion draft = pkg.createDraftVersion("{}");
        ContentSection a = section(draft, 1);
        section(draft, 2);

        assertThrows(IllegalArgumentException.class, () -> draft.reorderSections(List.of(a.getId())));
        assertThrows(IllegalArgumentException.class, () -> draft.reorderSections(List.of(a.getId(), a.getId())));
    }

    @Test
    void publishedVersionRefusesReorderAndRemoval() {
        ContentPackageVersion version = ContentPackageVersion.create(UUID.randomUUID(), 1, "{}");
        ContentSection a = ContentSection.create(version.getId(), "A", null, 1, null, null);
        version.addSection(a);
        version.publish(UUID.randomUUID());

        assertThrows(VersionNotDraftException.class, () -> version.reorderSections(List.of(a.getId())));
        assertThrows(VersionNotDraftException.class, () -> version.removeSection(a.getId()));
    }

    @Test
    void sortOrderAlreadyUsedInTheVersionIsRefused() {
        ContentPackage pkg = newPackage();
        ContentPackageVersion draft = pkg.createDraftVersion("{}");
        section(draft, 1);

        assertThrows(IllegalArgumentException.class,
                () -> draft.addSection(ContentSection.create(draft.getId(), "Clash", null, 1, null, null)));
    }

    @Test
    void removingASectionTakesItsQuestionsWithIt() {
        ContentPackage pkg = newPackage();
        ContentPackageVersion draft = pkg.createDraftVersion("{}");
        ContentSection a = section(draft, 1);

        draft.removeSection(a.getId());

        assertEquals(0, draft.getSections().size());
    }

    @Test
    void detailsChangeTitleAndAccessOnly() {
        ContentPackage pkg = newPackage();

        pkg.updateDetails("Renamed", "PREMIUM_CONTENT");

        assertEquals("Renamed", pkg.getTitle());
        assertEquals("PREMIUM_CONTENT", pkg.getRequiredFeatureKey());
        assertEquals(PackageType.PRACTICE_SET, pkg.getPackageType());
    }

    @Test
    void neverPublishedPackageIsArchivedAndItsScheduleIsDropped() {
        ContentPackage pkg = newPackage();
        ContentPackageVersion draft = pkg.createDraftVersion("{}");
        draft.schedulePublication(java.time.Instant.now().plusSeconds(3600));

        pkg.archive();

        assertEquals(PublicationStatus.ARCHIVED, pkg.getStatus());
        assertNull(draft.getPublishAt());
    }

    @Test
    void publishedPackageIsArchivedAndKeepsItsVersions() {
        ContentPackage pkg = newPackage();
        ContentPackageVersion published = publishedVersion(pkg.getId(), 1);
        pkg.addVersion(published);
        pkg.publishVersion(published.getId());

        pkg.archive();

        assertEquals(PublicationStatus.ARCHIVED, pkg.getStatus());
        assertEquals(1, pkg.getVersions().size());
        assertEquals(PublicationStatus.PUBLISHED, published.getStatus());
    }

    @Test
    void archivedPackageRefusesEditsAndPublishing() {
        ContentPackage pkg = newPackage();
        ContentPackageVersion draft = pkg.createDraftVersion("{}");
        pkg.archive();

        assertThrows(InvalidContentStateException.class, () -> pkg.updateDetails("Again", null));
        assertThrows(InvalidContentStateException.class, () -> pkg.publishVersion(draft.getId()));
        assertThrows(InvalidContentStateException.class, () -> pkg.createDraftVersion("{}"));
        assertThrows(InvalidContentStateException.class, pkg::archive);
    }

    private static ContentPackage newPackage() {
        return ContentPackage.create("PKG-" + UUID.randomUUID(), "Authoring test", PackageType.PRACTICE_SET, null);
    }

    private static ContentSection section(ContentPackageVersion version, int sortOrder) {
        ContentSection section = ContentSection.create(version.getId(), "Section " + sortOrder, null, sortOrder, null, null);
        version.addSection(section);
        return section;
    }

    private static ContentPackageVersion publishedVersion(UUID packageId, int number) {
        var now = java.time.Instant.now();
        return new ContentPackageVersion(UUID.randomUUID(), packageId, number, PublicationStatus.PUBLISHED, "{}", 1,
                now, UUID.randomUUID(), now, now, new ArrayList<>());
    }
}
