package com.ieltspath.content.application.usecase;

import com.ieltspath.commonstorage.key.InvalidObjectKeyException;
import com.ieltspath.content.application.port.SectionMediaWriter;
import com.ieltspath.content.application.result.AuthorPackageExtras;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.entity.ContentSection;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.exception.ContentSectionNotFoundException;
import com.ieltspath.content.domain.exception.VersionNotDraftException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.MediaReferencePolicy;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SectionMediaUseCasesTest {

    private final ContentPackageRepository packages = mock(ContentPackageRepository.class);
    private final SectionMediaWriter writer = mock(SectionMediaWriter.class);
    private final SectionMediaUseCases useCase = new SectionMediaUseCases(packages, writer,
            new MediaReferencePolicy("https://media.example.test/"));

    private final ContentPackage pkg = ContentPackage.create("MEDIA-1", "Media", PackageType.PRACTICE_SET, null);
    private final ContentPackageVersion draft = pkg.createDraftVersion("{}");
    private final ContentSection section = ContentSection.create(draft.getId(), "Passage", null, 1, null, null);

    {
        draft.addSection(section);
        when(packages.findByIdForUpdate(pkg.getId())).thenReturn(Optional.of(pkg));
    }

    @Test
    void passageIsStoredOnTheDraftSection() {
        String stored = useCase.setPassage(pkg.getId(), draft.getId(), section.getId(), "Some passage");

        assertEquals("Some passage", stored);
        verify(writer).setPassage(section.getId(), "Some passage");
    }

    @Test
    void blankOrHugePassageIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.setPassage(pkg.getId(), draft.getId(), section.getId(), "  "));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.setPassage(pkg.getId(), draft.getId(), section.getId(), "x".repeat(50_001)));
        verifyNoInteractions(writer);
    }

    @Test
    void passageCanBeCleared() {
        useCase.clearPassage(pkg.getId(), draft.getId(), section.getId());

        verify(writer).clearPassage(section.getId());
    }

    @Test
    void audioIsStoredWithItsResolvedUrl() {
        UUID asset = UUID.randomUUID();
        when(writer.setAudio(section.getId(), "listening/demo/ls1.mp3", 45, "words")).thenReturn(asset);

        AuthorPackageExtras.Audio audio = useCase.setAudio(pkg.getId(), draft.getId(), section.getId(),
                "listening/demo/ls1.mp3", 45, "words");

        assertEquals(asset, audio.assetId());
        assertEquals("https://media.example.test/listening/demo/ls1.mp3", audio.mediaUrl());
        assertEquals(45, audio.durationSeconds());
        assertEquals("words", audio.transcript());
    }

    @Test
    void blankTranscriptIsStoredAsNothing() {
        when(writer.setAudio(any(), anyString(), anyInt(), any())).thenReturn(UUID.randomUUID());

        AuthorPackageExtras.Audio audio = useCase.setAudio(pkg.getId(), draft.getId(), section.getId(),
                "listening/demo/ls1.mp3", 45, "   ");

        assertNull(audio.transcript());
        verify(writer).setAudio(section.getId(), "listening/demo/ls1.mp3", 45, null);
    }

    @Test
    void anUnsafeMediaReferenceIsRefusedBeforeAnythingIsWritten() {
        for (String reference : new String[]{"../secret.mp3", "/abs/path.mp3", "a//b.mp3", "javascript:alert(1)", ""}) {
            assertThrows(InvalidObjectKeyException.class, () -> useCase.setAudio(pkg.getId(), draft.getId(),
                    section.getId(), reference, 10, null), reference);
        }
        verifyNoInteractions(writer);
    }

    @Test
    void durationMustBeBetweenOneSecondAndTwoHours() {
        for (Integer duration : new Integer[]{null, 0, -1, 7201}) {
            assertThrows(IllegalArgumentException.class, () -> useCase.setAudio(pkg.getId(), draft.getId(),
                    section.getId(), "listening/demo/ls1.mp3", duration, null));
        }
        verifyNoInteractions(writer);
    }

    @Test
    void aPublishedVersionIsNeverChanged() {
        ContentPackage published = ContentPackage.create("MEDIA-2", "Published", PackageType.PRACTICE_SET, null);
        UUID versionId = UUID.randomUUID();
        ContentSection publishedSection = ContentSection.create(versionId, "S", null, 1, null, null);
        ContentPackageVersion version = new ContentPackageVersion(versionId, published.getId(), 1,
                PublicationStatus.PUBLISHED, "{}", 1, null, null, null, null, null, null,
                new java.util.ArrayList<>(java.util.List.of(publishedSection)));
        published.addVersion(version);
        when(packages.findByIdForUpdate(published.getId())).thenReturn(Optional.of(published));

        assertThrows(VersionNotDraftException.class,
                () -> useCase.setPassage(published.getId(), versionId, publishedSection.getId(), "text"));
        assertThrows(VersionNotDraftException.class,
                () -> useCase.clearAudio(published.getId(), versionId, publishedSection.getId()));
        verifyNoInteractions(writer);
    }

    @Test
    void aMissingSectionOrPackageIsNotFound() {
        ContentSection stranger = ContentSection.create(UUID.randomUUID(), "Other", null, 1, null, null);

        assertThrows(ContentSectionNotFoundException.class,
                () -> useCase.clearPassage(pkg.getId(), draft.getId(), stranger.getId()));
        UUID unknownPackage = UUID.randomUUID();
        when(packages.findByIdForUpdate(unknownPackage)).thenReturn(Optional.empty());
        assertThrows(ContentPackageNotFoundException.class,
                () -> useCase.clearPassage(unknownPackage, draft.getId(), section.getId()));
        verifyNoInteractions(writer);
    }
}
