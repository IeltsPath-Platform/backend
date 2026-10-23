package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.SaveContentSectionCommand;
import com.ieltspath.content.application.result.AuthorSectionResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentSection;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateContentSectionUseCase {

    private final ContentPackageRepository contentPackageRepository;

    public UpdateContentSectionUseCase(ContentPackageRepository contentPackageRepository) {
        this.contentPackageRepository = contentPackageRepository;
    }

    public AuthorSectionResult execute(SaveContentSectionCommand command) {
        ContentPackage pkg = contentPackageRepository.findByIdForUpdate(command.packageId())
                .orElseThrow(() -> new ContentPackageNotFoundException(command.packageId()));
        pkg.requireVersion(command.versionId()).requireDraft();
        ContentSection section = pkg.requireSectionInVersion(command.versionId(), command.sectionId());
        pkg.requireVersion(command.versionId()).requireFreeSortOrder(command.sortOrder(), section.getId());

        section.update(command.title(), command.skill(), command.sortOrder(), command.timeLimitSeconds(),
                command.instructions(), command.partNo(), command.coverAssetId());
        contentPackageRepository.save(pkg);
        return AuthorSectionResult.of(section);
    }
}
