package com.group01.content.application.usecase;

import com.group01.content.application.command.PublishContentPackageCommand;
import com.group01.content.application.port.LearningContentReader;
import com.group01.content.application.result.ContentPackageResult;
import com.group01.content.domain.exception.InvalidPackageLessonException;
import com.group01.content.domain.aggregate.ContentPackage;
import com.group01.content.domain.entity.ContentPackageVersion;
import com.group01.content.domain.exception.ContentPackageNotFoundException;
import com.group01.content.domain.exception.InvalidContentStateException;
import com.group01.content.domain.exception.QuestionAlreadyUsedException;
import com.group01.content.domain.exception.QuestionPurposeMismatchException;
import com.group01.content.domain.repository.ContentPackageRepository;
import com.group01.content.domain.vo.QuestionUsageConflict;
import com.group01.content.domain.vo.QuestionPurpose;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

        switch (pkg.getPackageType()) {
            case PRACTICE_SET, TOPIC_TEST, MOCK_TEST, PLACEMENT_TEST -> {
                lessons.lockQuestionsForPublishing(targetVersion.getId());
                List<QuestionUsageConflict> conflicts = lessons.questionsUsedElsewhere(targetVersion.getId());
                if (!conflicts.isEmpty()) {
                    throw new QuestionAlreadyUsedException(conflicts);
                }
                QuestionPurpose requiredPurpose = switch (pkg.getPackageType()) {
                    case MOCK_TEST, PLACEMENT_TEST -> QuestionPurpose.EXAM;
                    default -> QuestionPurpose.LEARNING;
                };
                List<UUID> wrongPurpose = lessons.questionsWithWrongPurpose(targetVersion.getId(), requiredPurpose);
                if (!wrongPurpose.isEmpty()) {
                    throw new QuestionPurposeMismatchException(requiredPurpose, wrongPurpose);
                }
            }
            default -> { }
        }

        targetVersion.publish(command.publishedBy());
        pkg.publishVersion(targetVersion.getId());

        return ContentPackageResult.of(contentPackageRepository.save(pkg));
    }
}
