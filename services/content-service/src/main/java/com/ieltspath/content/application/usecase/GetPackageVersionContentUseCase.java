package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.PackageVersionContentResult;
import com.ieltspath.content.domain.exception.PackageVersionNotFoundException;
import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.MediaReferencePolicy;
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
