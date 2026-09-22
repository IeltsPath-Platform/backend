package com.group01.content.application.usecase;

import com.group01.content.application.command.AddContentSectionCommand;
import com.group01.content.application.result.ContentSectionResult;
import com.group01.content.domain.aggregate.ContentPackage;
import com.group01.content.domain.entity.ContentPackageVersion;
import com.group01.content.domain.entity.ContentSection;
import com.group01.content.domain.exception.ContentPackageNotFoundException;
import com.group01.content.domain.repository.ContentPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AddContentSectionUseCase {

    private final ContentPackageRepository contentPackageRepository;

    public AddContentSectionUseCase(ContentPackageRepository contentPackageRepository) {
        this.contentPackageRepository = contentPackageRepository;
    }

    public ContentSectionResult execute(AddContentSectionCommand command) {
        // Find package owning this version
        List<ContentPackage> packages = contentPackageRepository.findAll(null, null);
        ContentPackage targetPkg = null;
        ContentPackageVersion targetVersion = null;

        for (ContentPackage p : packages) {
            for (ContentPackageVersion v : p.getVersions()) {
                if (v.getId().equals(command.packageVersionId())) {
                    targetPkg = p;
                    targetVersion = v;
                    break;
                }
            }
            if (targetVersion != null) break;
        }

        if (targetVersion == null) {
            throw new ContentPackageNotFoundException("Package version not found: " + command.packageVersionId());
        }

        ContentSection section = ContentSection.create(
                command.packageVersionId(),
                command.title(),
                command.skill(),
                command.sortOrder(),
                command.timeLimitSeconds(),
                command.instructions()
        );

        targetVersion.addSection(section);
        contentPackageRepository.save(targetPkg);

        return new ContentSectionResult(
                section.getId(),
                section.getPackageVersionId(),
                section.getTitle(),
                section.getSkill(),
                section.getSortOrder(),
                section.getTimeLimitSeconds(),
                section.getInstructions(),
                section.getCreatedAt(),
                section.getUpdatedAt(),
                List.of()
        );
    }
}

