package com.ieltspath.content.domain.entity;

import com.ieltspath.content.domain.exception.VersionNotDraftException;
import com.ieltspath.content.domain.vo.PublicationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContentPackageVersionTest {

    @Test
    void draftVersionAcceptsSections() {
        ContentPackageVersion draft = ContentPackageVersion.create(UUID.randomUUID(), 1, "{}");

        draft.addSection(section(draft.getId()));

        assertEquals(1, draft.getSections().size());
    }

    @Test
    void publishedVersionRefusesSections() {
        ContentPackageVersion published = publishedVersion();

        assertThrows(VersionNotDraftException.class, () -> published.addSection(section(published.getId())));
        assertEquals(0, published.getSections().size());
    }

    private static ContentPackageVersion publishedVersion() {
        Instant now = Instant.now();
        return new ContentPackageVersion(UUID.randomUUID(), UUID.randomUUID(), 1, PublicationStatus.PUBLISHED,
                "{}", 1, now, UUID.randomUUID(), now, now, new ArrayList<>());
    }

    private static ContentSection section(UUID versionId) {
        return ContentSection.create(versionId, "Reading Passage 1", null, 1, 1200, null, 1, null);
    }
}
