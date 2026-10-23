package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateContentPackageCommand;
import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.AuthorPackageResult;
import com.ieltspath.content.application.result.ContentPackageResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.exception.DuplicateCodeException;
import com.ieltspath.content.domain.exception.InvalidPackageCourseException;
import com.ieltspath.content.domain.exception.InvalidPackageLessonException;
import com.ieltspath.content.domain.exception.InvalidPackageTopicException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.PackageType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a package. The creation rules live here once: {@link #execute} serves the original route, which never
 * creates final tests, and {@link #executeAsAuthor} serves the authoring route, which also creates topic and course
 * final tests bound to an existing topic or course.
 */
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
        return ContentPackageResult.of(create(command));
    }

    public AuthorPackageResult executeAsAuthor(CreateContentPackageCommand command) {
        return AuthorPackageResult.of(create(command));
    }

    private ContentPackage create(CreateContentPackageCommand command) {
        if (command.packageType() == PackageType.LESSON) {
            throw new IllegalArgumentException("LESSON packages cannot be created through this API");
        }
        if (contentPackageRepository.existsByCode(command.code())) {
            throw new DuplicateCodeException("ContentPackage", command.code());
        }
        requireBinding(command);

        if (command.lessonId() != null && !lessons.lessonExists(command.lessonId())) {
            throw new InvalidPackageLessonException("Lesson " + command.lessonId() + " does not exist");
        }
        // A practice test without a source is internal (a course checkpoint) and is not listed in the library.
        if (command.packageType() != PackageType.PRACTICE_TEST && command.sourceCode() != null) {
            throw new IllegalArgumentException("sourceCode is only valid for PRACTICE_TEST");
        }
        if (command.sourceCode() != null) {
            try { com.ieltspath.content.domain.vo.TestSource.valueOf(command.sourceCode()); }
            catch (RuntimeException invalidSource) { throw new IllegalArgumentException("sourceCode is not a supported test source"); }
        }

        ContentPackage pkg = command.packageType() == PackageType.TOPIC_TEST
                || command.packageType() == PackageType.COURSE_TEST
                ? ContentPackage.createFinalTest(command.code(), command.title(), command.packageType(),
                        command.requiredFeatureKey(), command.topicId(), command.courseId())
                : ContentPackage.create(command.code(), command.title(), command.packageType(),
                        command.requiredFeatureKey(), command.lessonId(), command.sourceCode());
        return contentPackageRepository.save(pkg);
    }

    /** A final test is bound to exactly its topic or course; every other package type is bound to neither. */
    private void requireBinding(CreateContentPackageCommand command) {
        switch (command.packageType()) {
            case TOPIC_TEST -> {
                if (command.topicId() == null || command.lessonId() != null || command.courseId() != null
                        || command.sourceCode() != null) {
                    throw new InvalidPackageTopicException(
                            "A TOPIC_TEST needs a topicId and no lessonId, courseId or sourceCode");
                }
                if (!lessons.topicExists(command.topicId())) {
                    throw new InvalidPackageTopicException("Topic " + command.topicId() + " does not exist");
                }
            }
            case COURSE_TEST -> {
                if (command.courseId() == null || command.lessonId() != null || command.topicId() != null
                        || command.sourceCode() != null) {
                    throw new InvalidPackageCourseException(
                            "A COURSE_TEST needs a courseId and no lessonId, topicId or sourceCode");
                }
                if (!lessons.courseExists(command.courseId())) {
                    throw new InvalidPackageCourseException("Course " + command.courseId() + " does not exist");
                }
            }
            default -> {
                if (command.topicId() != null || command.courseId() != null) {
                    throw new IllegalArgumentException("topicId and courseId are only valid for final tests");
                }
            }
        }
    }
}
