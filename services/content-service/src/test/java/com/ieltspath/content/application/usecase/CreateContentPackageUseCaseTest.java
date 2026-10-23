package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateContentPackageCommand;
import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.domain.exception.InvalidPackageCourseException;
import com.ieltspath.content.domain.exception.InvalidPackageTopicException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.PackageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A practice test without a source is an internal course checkpoint; a source, when given, must be supported. */
class CreateContentPackageUseCaseTest {
    private final ContentPackageRepository packages = mock(ContentPackageRepository.class);
    private final LearningContentReader reader = mock(LearningContentReader.class);
    private final CreateContentPackageUseCase useCase = new CreateContentPackageUseCase(packages, reader);

    private void savingEchoesThePackage() {
        when(packages.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void aPracticeTestCanBeCreatedWithoutASourceAsAnInternalCheckpoint() {
        savingEchoesThePackage();

        var created = useCase.execute(new CreateContentPackageCommand("CHECKPOINT-1", "Checkpoint",
                PackageType.PRACTICE_TEST, null, null, null));

        assertThat(created.sourceCode()).isNull();
        assertThat(created.packageType()).isEqualTo(PackageType.PRACTICE_TEST);
    }

    @Test
    void aPracticeTestWithASupportedSourceIsCreatedForTheLibrary() {
        savingEchoesThePackage();

        var created = useCase.execute(new CreateContentPackageCommand("LIBRARY-1", "Library test",
                PackageType.PRACTICE_TEST, null, null, "IELTS_SPACE_PRO"));

        assertThat(created.sourceCode()).isEqualTo("IELTS_SPACE_PRO");
    }

    @Test
    void anUnknownSourceIsRefused() {
        assertThatThrownBy(() -> useCase.execute(new CreateContentPackageCommand("BAD-SOURCE", "Bad",
                PackageType.PRACTICE_TEST, null, null, "NOT_A_SOURCE")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sourceCode");
        verify(packages, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class, names = {"PRACTICE_SET", "QUIZ", "MOCK_TEST", "PLACEMENT_TEST"})
    void aSourceOnAnyOtherPackageTypeIsRefused(PackageType type) {
        assertThatThrownBy(() -> useCase.execute(new CreateContentPackageCommand("OTHER-" + type, "Other", type, null,
                null, "IELTS_SPACE_PRO")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("only valid for PRACTICE_TEST");
        verify(packages, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class, names = {"TOPIC_TEST", "COURSE_TEST"})
    void theOriginalRouteStillRefusesFinalTests(PackageType type) {
        assertThatThrownBy(() -> useCase.execute(new CreateContentPackageCommand("FINAL-" + type, "Final", type, null,
                null, null, UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cannot be created");
        verify(packages, never()).save(any());
    }

    @Test
    void anAuthorCreatesATopicTestBoundToAnExistingTopic() {
        savingEchoesThePackage();
        UUID topicId = UUID.randomUUID();
        when(reader.topicExists(topicId)).thenReturn(true);

        var created = useCase.executeAsAuthor(new CreateContentPackageCommand("TT-1", "Topic test",
                PackageType.TOPIC_TEST, null, null, null, topicId, null));

        assertThat(created.topicId()).isEqualTo(topicId);
        assertThat(created.status()).isEqualTo(com.ieltspath.content.domain.vo.PublicationStatus.DRAFT);
        assertThat(created.versions()).isEmpty();
    }

    @Test
    void aTopicTestNeedsAnExistingTopicAndNothingElse() {
        UUID topicId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        when(reader.topicExists(topicId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.executeAsAuthor(command("TT-NONE", PackageType.TOPIC_TEST, null, null)))
                .isInstanceOf(InvalidPackageTopicException.class);
        assertThatThrownBy(() -> useCase.executeAsAuthor(command("TT-MISSING", PackageType.TOPIC_TEST, topicId, null)))
                .isInstanceOf(InvalidPackageTopicException.class);
        when(reader.topicExists(topicId)).thenReturn(true);
        assertThatThrownBy(() -> useCase.executeAsAuthor(new CreateContentPackageCommand("TT-LESSON", "T",
                PackageType.TOPIC_TEST, null, lessonId, null, topicId, null)))
                .isInstanceOf(InvalidPackageTopicException.class);
        assertThatThrownBy(() -> useCase.executeAsAuthor(command("TT-COURSE", PackageType.TOPIC_TEST, topicId,
                UUID.randomUUID()))).isInstanceOf(InvalidPackageTopicException.class);
        verify(packages, never()).save(any());
    }

    @Test
    void anAuthorCreatesACourseTestBoundToAnExistingCourse() {
        savingEchoesThePackage();
        UUID courseId = UUID.randomUUID();
        when(reader.courseExists(courseId)).thenReturn(true);

        var created = useCase.executeAsAuthor(command("CT-1", PackageType.COURSE_TEST, null, courseId));

        assertThat(created.courseId()).isEqualTo(courseId);
    }

    @Test
    void aCourseTestNeedsAnExistingCourseAndNothingElse() {
        UUID courseId = UUID.randomUUID();
        when(reader.courseExists(courseId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.executeAsAuthor(command("CT-NONE", PackageType.COURSE_TEST, null, null)))
                .isInstanceOf(InvalidPackageCourseException.class);
        assertThatThrownBy(() -> useCase.executeAsAuthor(command("CT-MISSING", PackageType.COURSE_TEST, null, courseId)))
                .isInstanceOf(InvalidPackageCourseException.class);
        when(reader.courseExists(courseId)).thenReturn(true);
        assertThatThrownBy(() -> useCase.executeAsAuthor(command("CT-TOPIC", PackageType.COURSE_TEST,
                UUID.randomUUID(), courseId))).isInstanceOf(InvalidPackageCourseException.class);
        verify(packages, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = PackageType.class, names = {"PRACTICE_SET", "QUIZ", "MOCK_TEST", "PLACEMENT_TEST",
            "PRACTICE_TEST"})
    void otherTypesRefuseATopicOrCourse(PackageType type) {
        assertThatThrownBy(() -> useCase.executeAsAuthor(command("T-" + type, type, UUID.randomUUID(), null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("final tests");
        assertThatThrownBy(() -> useCase.executeAsAuthor(command("C-" + type, type, null, UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("final tests");
        verify(packages, never()).save(any());
    }

    @Test
    void aLessonPackageIsNeverCreatedHere() {
        assertThatThrownBy(() -> useCase.executeAsAuthor(command("L-1", PackageType.LESSON, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aDuplicateCodeIsRefused() {
        when(packages.existsByCode("DUP")).thenReturn(true);

        assertThatThrownBy(() -> useCase.executeAsAuthor(command("DUP", PackageType.QUIZ, null, null)))
                .isInstanceOf(com.ieltspath.content.domain.exception.DuplicateCodeException.class);
    }

    private static CreateContentPackageCommand command(String code, PackageType type, UUID topicId, UUID courseId) {
        return new CreateContentPackageCommand(code, "Title", type, null, null, null, topicId, courseId);
    }
}
