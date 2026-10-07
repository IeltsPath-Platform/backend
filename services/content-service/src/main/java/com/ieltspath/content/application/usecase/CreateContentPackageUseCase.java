package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateContentPackageCommand;
import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.ContentPackageResult;
import com.ieltspath.content.domain.exception.InvalidPackageLessonException;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.exception.DuplicateCodeException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.PackageType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateContentPackageUseCase {

    private final ContentPackageRepository contentPackageRepository;
    private final LearningContentReader lessons;

    public CreateContentPackageUseCase(ContentPackageRepository contentPackageRepository,
                                       LearningContentReader lessons) {
        this.contentPackageRepository = contentPackageRepository;
        this.lessons = lessons;
    }

    public ContentPackageResult execute(CreateContentPackageCommand command) {
        if (command.packageType() == PackageType.TOPIC_TEST || command.packageType() == PackageType.COURSE_TEST) {
            // Final tests belong to the seeded curriculum.
            throw new IllegalArgumentException(command.packageType() + " packages cannot be created through this API");
        }
        if (contentPackageRepository.existsByCode(command.code())) {
            throw new DuplicateCodeException("ContentPackage", command.code());
        }

        if (command.lessonId() != null && !lessons.lessonExists(command.lessonId())) {
            throw new InvalidPackageLessonException("Lesson " + command.lessonId() + " does not exist");
        }

        ContentPackage pkg = ContentPackage.create(
                command.code(),
                command.title(),
                command.packageType(),
                command.requiredFeatureKey(),
                command.lessonId()
        );

        return ContentPackageResult.of(contentPackageRepository.save(pkg));
    }
}
