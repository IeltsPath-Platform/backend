package com.group01.content.application.usecase;

import com.group01.content.application.port.LearningContentReader;
import com.group01.content.application.result.PackageVersionContentResult;
import com.group01.content.domain.exception.PackageVersionNotFoundException;
import com.group01.content.domain.vo.AssetType;
import com.group01.content.domain.vo.MediaReferencePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetPackageVersionContentUseCase {
    private final LearningContentReader reader;
    private final MediaReferencePolicy mediaPolicy;

    public GetPackageVersionContentUseCase(LearningContentReader reader, MediaReferencePolicy mediaPolicy) {
        this.reader = reader;
        this.mediaPolicy = mediaPolicy;
    }

    public PackageVersionContentResult execute(UUID packageVersionId) {
        PackageVersionContentResult version = reader.publishedPackageVersion(packageVersionId)
                .orElseThrow(() -> new PackageVersionNotFoundException(packageVersionId));
        return version.withSections(version.sections().stream()
                .map(s -> s.audio() == null ? s : s.withAudio(s.audio().withMediaUrl(
                        mediaPolicy.resolve(AssetType.AUDIO, s.audio().mediaReference()))))
                .toList());
    }
}
