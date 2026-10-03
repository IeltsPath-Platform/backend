package com.group01.content.application.usecase;

import com.group01.content.application.command.PublishContentPackageCommand;
import com.group01.content.application.port.LearningContentReader;
import com.group01.content.application.result.ContentPackageResult;
import com.group01.content.domain.exception.InvalidPackageLessonException;
import com.group01.content.domain.aggregate.ContentPackage;
import com.group01.content.domain.entity.ContentPackageVersion;
import com.group01.content.domain.exception.ContentPackageNotFoundException;
import com.group01.content.domain.exception.InvalidContentStateException;
import com.group01.content.domain.repository.ContentPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PublishContentPackageUseCase {

    private final ContentPackageRepository contentPackageRepository;
    private final LearningContentReader lessons;

    public PublishContentPackageUseCase(ContentPackageRepository contentPackageRepository,
                                        LearningContentReader lessons) {
        this.contentPackageRepository = contentPackageRepository;
        this.lessons = lessons;
    }

    public ContentPackageResult execute(PublishContentPackageCommand command) {
        ContentPackage pkg = contentPackageRepository.findById(command.packageId())
                .orElseThrow(() -> new ContentPackageNotFoundException(command.packageId()));

        ContentPackageVersion targetVersion = null;
        for (ContentPackageVersion v : pkg.getVersions()) {
            if (v.getId().equals(command.versionId())) {
                targetVersion = v;
                break;
            }
        }

        if (targetVersion == null) {
            throw new InvalidContentStateException("Version " + command.versionId() + " does not belong to package " + command.packageId());
        }

        // A lesson's Practice teaches that lesson's skill only.
        if (pkg.getLessonId() != null
                && lessons.packageVersionLeavesLessonSkill(targetVersion.getId(), pkg.getLessonId())) {
            throw new InvalidPackageLessonException("Every question of a lesson's practice set must have the skill "
                    + "of the lesson's topic");
        }

        targetVersion.publish(command.publishedBy());
        pkg.publishVersion(targetVersion.getId());

        return ContentPackageResult.of(contentPackageRepository.save(pkg));
    }
}
