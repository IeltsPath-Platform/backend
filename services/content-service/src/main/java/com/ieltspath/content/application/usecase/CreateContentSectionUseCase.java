package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.SaveContentSectionCommand;
import com.ieltspath.content.application.result.AuthorSectionResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.entity.ContentSection;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateContentSectionUseCase {

    private final ContentPackageRepository contentPackageRepository;

    public CreateContentSectionUseCase(ContentPackageRepository contentPackageRepository) {
        this.contentPackageRepository = contentPackageRepository;
    }

    public AuthorSectionResult execute(SaveContentSectionCommand command) {
        ContentPackage pkg = contentPackageRepository.findByIdForUpdate(command.packageId())
                .orElseThrow(() -> new ContentPackageNotFoundException(command.packageId()));
        ContentPackageVersion version = pkg.requireVersion(command.versionId());
        version.requireDraft();

        ContentSection section = ContentSection.create(version.getId(), command.title(), command.skill(),
                command.sortOrder(), command.timeLimitSeconds(), command.instructions(), command.partNo(),
                command.coverAssetId());
        version.addSection(section);
        contentPackageRepository.save(pkg);
        return AuthorSectionResult.of(section);
    }
}
