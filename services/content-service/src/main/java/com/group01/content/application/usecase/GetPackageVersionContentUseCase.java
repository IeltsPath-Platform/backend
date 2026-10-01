package com.group01.content.application.usecase;

import com.group01.content.application.port.LearningContentReader;
import com.group01.content.application.result.PackageVersionContentResult;
import com.group01.content.domain.exception.PackageVersionNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetPackageVersionContentUseCase {
    private final LearningContentReader reader;

    public GetPackageVersionContentUseCase(LearningContentReader reader) {
        this.reader = reader;
    }

    public PackageVersionContentResult execute(UUID packageVersionId) {
        return reader.publishedPackageVersion(packageVersionId)
                .orElseThrow(() -> new PackageVersionNotFoundException(packageVersionId));
    }
}
