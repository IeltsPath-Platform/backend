package com.ieltspath.content.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.content.application.port.SectionMediaWriter;
import com.ieltspath.content.application.result.AuthorPackageExtras;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.MediaReferencePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Sets and clears the passage and audio of a section of a draft version. */
@Service
@Transactional
public class SectionMediaUseCases {

    static final int MAX_PASSAGE_CHARS = 50_000;
    static final int MAX_TRANSCRIPT_CHARS = 50_000;
    static final int MAX_AUDIO_SECONDS = 7200;

    private final ContentPackageRepository packages;
    private final SectionMediaWriter writer;
    private final MediaReferencePolicy mediaPolicy;

    public SectionMediaUseCases(ContentPackageRepository packages, SectionMediaWriter writer,
                                MediaReferencePolicy mediaPolicy) {
        this.packages = packages;
        this.writer = writer;
        this.mediaPolicy = mediaPolicy;
    }

    /** @return the stored passage */
    public String setPassage(UUID packageId, UUID versionId, UUID sectionId, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("passage text must not be blank");
        }
        if (text.length() > MAX_PASSAGE_CHARS) {
            throw new IllegalArgumentException("passage text must not exceed " + MAX_PASSAGE_CHARS + " characters");
        }
        writer.setPassage(draftSection(packageId, versionId, sectionId), text);
        return text;
    }

    public void clearPassage(UUID packageId, UUID versionId, UUID sectionId) {
        writer.clearPassage(draftSection(packageId, versionId, sectionId));
    }

    public AuthorPackageExtras.Audio setAudio(UUID packageId, UUID versionId, UUID sectionId, String mediaReference,
                                              Integer durationSeconds, String transcript) {
        ObjectKeys.requireSafe(mediaReference);
        if (durationSeconds == null || durationSeconds < 1 || durationSeconds > MAX_AUDIO_SECONDS) {
            throw new IllegalArgumentException("durationSeconds must be between 1 and " + MAX_AUDIO_SECONDS);
        }
        if (transcript != null && transcript.length() > MAX_TRANSCRIPT_CHARS) {
            throw new IllegalArgumentException("transcript must not exceed " + MAX_TRANSCRIPT_CHARS + " characters");
        }
        UUID section = draftSection(packageId, versionId, sectionId);
        String storedTranscript = transcript == null || transcript.isBlank() ? null : transcript;
        UUID assetId = writer.setAudio(section, mediaReference, durationSeconds, storedTranscript);
        String url;
        try {
            url = mediaPolicy.resolve(AssetType.AUDIO, mediaReference);
        } catch (RuntimeException unresolvable) {
            url = null;
        }
        return new AuthorPackageExtras.Audio(assetId, mediaReference, url, durationSeconds, storedTranscript);
    }

    public void clearAudio(UUID packageId, UUID versionId, UUID sectionId) {
        writer.clearAudio(draftSection(packageId, versionId, sectionId));
    }

    /** Locks the package so a media change cannot race a publication, and refuses anything but a draft section. */
    private UUID draftSection(UUID packageId, UUID versionId, UUID sectionId) {
        ContentPackage pkg = packages.findByIdForUpdate(packageId)
                .orElseThrow(() -> new ContentPackageNotFoundException(packageId));
        pkg.requireVersion(versionId).requireDraft();
        return pkg.requireSectionInVersion(versionId, sectionId).getId();
    }
}
