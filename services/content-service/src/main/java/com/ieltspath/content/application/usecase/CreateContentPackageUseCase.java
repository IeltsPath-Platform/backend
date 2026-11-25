package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateContentPackageCommand;
import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.AuthorPackageResult;
import com.ieltspath.content.application.result.ContentPackageResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.exception.DuplicateCodeException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
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
            throw new IllegalArgumentException(command.packageType() + " gói không tạo được qua chức năng này");
        }
        return ContentPackageResult.of(create(command));
    }

    public AuthorPackageResult executeAsAuthor(CreateContentPackageCommand command) {
        return AuthorPackageResult.of(create(command));
    }

    private ContentPackage create(CreateContentPackageCommand command) {
        if ((command.packageType() == PackageType.PLACEMENT_TEST) != (command.placementPool() != null)) {
            if (command.packageType() == PackageType.PLACEMENT_TEST) throw InvalidContentStateException.placementPoolRequired();
            throw new IllegalArgumentException("Nhóm đề xếp lớp chỉ dùng cho đề xếp lớp (PLACEMENT_TEST)");
        }
        if (command.packageType() == PackageType.LESSON) {
            throw new IllegalArgumentException("Gói bài học (LESSON) không tạo được qua chức năng này");
        }
        if (contentPackageRepository.existsByCode(command.code())) {
            throw new DuplicateCodeException("ContentPackage", command.code());
        }
        requireBinding(command);

        if (command.lessonId() != null && !lessons.lessonExists(command.lessonId())) {
            throw new InvalidPackageLessonException("Bài học không tồn tại");
        }
        // A practice test without a source is internal (a course checkpoint) and is not listed in the library.
        if (command.packageType() != PackageType.PRACTICE_TEST && command.sourceCode() != null) {
            throw new IllegalArgumentException("Nguồn đề (sourceCode) chỉ dùng cho bài kiểm tra luyện tập (PRACTICE_TEST)");
        }
        if (command.sourceCode() != null) {
            try { com.ieltspath.content.domain.vo.TestSource.valueOf(command.sourceCode()); }
            catch (RuntimeException invalidSource) { throw new IllegalArgumentException("Nguồn đề không được hỗ trợ"); }
        }

        ContentPackage pkg = command.packageType() == PackageType.TOPIC_TEST
                || command.packageType() == PackageType.COURSE_TEST
                ? ContentPackage.createFinalTest(command.code(), command.title(), command.packageType(),
                        command.requiredFeatureKey(), command.topicId(), command.courseId())
                : ContentPackage.create(command.code(), command.title(), command.packageType(),
                        command.requiredFeatureKey(), command.lessonId(), command.sourceCode(), command.placementPool());
        return contentPackageRepository.save(pkg);
    }

    /** A final test is bound to exactly its topic or course; every other package type is bound to neither. */
    private void requireBinding(CreateContentPackageCommand command) {
        switch (command.packageType()) {
            case TOPIC_TEST -> {
                if (command.topicId() == null || command.lessonId() != null || command.courseId() != null
                        || command.sourceCode() != null) {
                    throw new InvalidPackageTopicException(
                            "Đề cuối chủ đề cần một chủ đề và không được gắn bài học, khóa học hay nguồn đề");
                }
                if (!lessons.topicExists(command.topicId())) {
                    throw new InvalidPackageTopicException("Chủ đề không tồn tại");
                }
            }
            case COURSE_TEST -> {
                if (command.courseId() == null || command.lessonId() != null || command.topicId() != null
                        || command.sourceCode() != null) {
                    throw new InvalidPackageCourseException(
                            "Đề cuối khóa cần một khóa học và không được gắn bài học, chủ đề hay nguồn đề");
                }
                if (!lessons.courseExists(command.courseId())) {
                    throw new InvalidPackageCourseException("Khóa học không tồn tại");
                }
            }
            default -> {
                if (command.topicId() != null || command.courseId() != null) {
                    throw new IllegalArgumentException("Chủ đề và khóa học chỉ dùng cho đề cuối chủ đề và đề cuối khóa");
                }
            }
        }
    }
}
