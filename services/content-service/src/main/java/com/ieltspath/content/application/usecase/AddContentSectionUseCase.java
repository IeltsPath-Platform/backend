package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.AddContentSectionCommand;
import com.ieltspath.content.application.result.ContentSectionResult;
import com.ieltspath.content.domain.entity.ContentSection;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
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
        var targetPackage = contentPackageRepository.findByVersionId(command.packageVersionId())
                .orElseThrow(() -> new ContentPackageNotFoundException(
                        "Package version not found: " + command.packageVersionId()));
        var targetVersion = targetPackage.getVersions().stream()
                .filter(version -> version.getId().equals(command.packageVersionId()))
                .findFirst()
                .orElseThrow(() -> new ContentPackageNotFoundException(
                        "Package version not found: " + command.packageVersionId()));

        ContentSection section = ContentSection.create(
                command.packageVersionId(),
                command.title(),
                command.skill(),
                command.sortOrder(),
                command.timeLimitSeconds(),
                command.instructions()
        );

        targetVersion.addSection(section);
        contentPackageRepository.save(targetPackage);

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
