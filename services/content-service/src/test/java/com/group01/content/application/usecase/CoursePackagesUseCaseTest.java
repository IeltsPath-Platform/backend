package com.group01.content.application.usecase;

import com.group01.content.application.command.*;
import com.group01.content.application.port.LearningContentReader;
import com.group01.content.application.result.PackageQuestionSpec;
import com.group01.content.application.result.TopicTestPackageResult;
import com.group01.content.domain.aggregate.*;
import com.group01.content.domain.entity.ContentPackageVersion;
import com.group01.content.domain.exception.*;
import com.group01.content.domain.repository.*;
import com.group01.content.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CoursePackagesUseCaseTest {
    private final LearningContentReader reader = mock(LearningContentReader.class);
    private final ContentPackageRepository packages = mock(ContentPackageRepository.class);

    @Test
    void fillAlternativesUseTheGradersUnicodeWhitespaceRule() {
        ContentPackage valid = draft();
        when(reader.packageQuestionSpecs(valid.getVersions().getFirst().getId())).thenReturn(List.of(
                new PackageQuestionSpec(UUID.randomUUID(), QuestionType.FILL_IN_BLANK, Skill.READING,
                        "{\"type\":\"FILL\",\"accepted\":[\"\u00a0answer\u00a0\"]}")));
        when(packages.save(any())).thenAnswer(call -> call.getArgument(0));
        assertThat(publish(valid).status()).isEqualTo(PublicationStatus.PUBLISHED);
        ContentPackage invalid = draft();
        when(reader.packageQuestionSpecs(invalid.getVersions().getFirst().getId())).thenReturn(List.of(
                new PackageQuestionSpec(UUID.randomUUID(), QuestionType.FILL_IN_BLANK, Skill.READING,
                        "{\"type\":\"FILL\",\"accepted\":[\"\u00a0\"]}")));
        assertThatThrownBy(() -> publish(invalid)).isInstanceOf(InvalidContentStateException.class);
        assertThat(invalid.getStatus()).isEqualTo(PublicationStatus.DRAFT);
    }

    @Test
    void emptyCourseTestsCannotPublish() {
        ContentPackage empty = draft();
        assertThatThrownBy(() -> publish(empty)).isInstanceOf(InvalidContentStateException.class);
        verify(packages, never()).save(any());
    }

    @Test
    void courseTestsAreSeededOnlyAndRequireCourseMembership() {
        assertThatThrownBy(() -> new CreateContentPackageUseCase(packages, reader).execute(
                new CreateContentPackageCommand("FINAL", "Final", PackageType.COURSE_TEST, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ContentPackage.create("FINAL", "Final", PackageType.COURSE_TEST, null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(packages);
    }

    @Test
    void coursePackagesAreReadOnlyForActiveCourses() {
        var courses = mock(CourseRepository.class);
        Course course = Course.create("IELTS", "IELTS", new BigDecimal("5.5"));
        when(courses.findById(course.getId())).thenReturn(Optional.of(course));
        var result = new TopicTestPackageResult(UUID.randomUUID(), UUID.randomUUID(), "FINAL");
        when(reader.courseTestPackages(course.getId())).thenReturn(List.of(result));
        var useCase = new GetCourseTestPackagesUseCase(reader, courses);
        assertThat(useCase.execute(course.getId())).containsExactly(result);
        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID())).isInstanceOf(CourseNotFoundException.class);
        course.update("IELTS", course.getBandLevel(), ContentStatus.INACTIVE);
        assertThatThrownBy(() -> useCase.execute(course.getId())).isInstanceOf(CourseNotFoundException.class);
        verify(reader, times(1)).courseTestPackages(any());
    }

    @Test
    void coursePublishingLocksQuestionsAndEnforcesLearningPurposeAndOwnership() {
        ContentPackage pkg = draft();
        UUID version = pkg.getVersions().getFirst().getId();
        when(reader.questionsWithWrongPurpose(version, QuestionPurpose.LEARNING)).thenReturn(List.of(UUID.randomUUID()));
        assertThatThrownBy(() -> publish(pkg)).isInstanceOf(QuestionPurposeMismatchException.class);
        var order = inOrder(reader);
        order.verify(reader).lockQuestionsForPublishing(version);
        order.verify(reader).questionsUsedElsewhere(version);
        order.verify(reader).questionsWithWrongPurpose(version, QuestionPurpose.LEARNING);
        assertThat(pkg.getStatus()).isEqualTo(PublicationStatus.DRAFT);
        verify(packages, never()).save(any());
        reset(reader);
        when(reader.questionsUsedElsewhere(version)).thenReturn(List.of(new QuestionUsageConflict(UUID.randomUUID(),
                QuestionUsageConflict.OwnerType.PACKAGE, "OTHER")));
        assertThatThrownBy(() -> publish(pkg)).isInstanceOf(QuestionAlreadyUsedException.class);
        verify(packages, never()).save(any());
    }

    @Test
    void ungradableCourseQuestionsAreRejectedBeforePublication() {
        for (String spec : List.of("{}", "{\"type\":\"ESSAY\"}", "{\"type\":\"SPEAKING\"}",
                "{\"type\":\"CHOICE\",\"correct\":1}", "{\"type\":\"CHOICE\",\"correct\":\" \"}",
                "{\"type\":\"FILL\",\"accepted\":[]}", "{\"type\":\"FILL\",\"accepted\":[\"x\",1]}",
                "{\"type\":\"FILL\",\"accepted\":[\" \" ]}", "[]", "null", "invalid")) {
            ContentPackage pkg = draft();
            when(reader.packageQuestionSpecs(pkg.getVersions().getFirst().getId())).thenReturn(List.of(
                    new PackageQuestionSpec(UUID.randomUUID(), QuestionType.MULTIPLE_CHOICE, Skill.READING, spec)));
            assertThatThrownBy(() -> publish(pkg)).isInstanceOf(InvalidContentStateException.class);
            assertThat(pkg.getStatus()).isEqualTo(PublicationStatus.DRAFT);
            assertThat(pkg.getVersions().getFirst().getStatus()).isEqualTo(PublicationStatus.DRAFT);
        }
        verify(packages, never()).save(any());
    }

    @Test
    void validChoiceFillAndLegacyChoiceSpecsPublishButEssayOrNonReadingDoNot() {
        for (String spec : List.of("{\"type\":\"CHOICE\",\"correct\":\"A\"}",
                "{\"correct\":\"A\"}", "{\"type\":\"FILL\",\"accepted\":[\"answer\"]}")) {
            ContentPackage pkg = draft();
            when(reader.packageQuestionSpecs(pkg.getVersions().getFirst().getId())).thenReturn(List.of(
                    new PackageQuestionSpec(UUID.randomUUID(), QuestionType.MULTIPLE_CHOICE, Skill.READING, spec)));
            when(packages.save(any())).thenAnswer(call -> call.getArgument(0));
            assertThat(publish(pkg).status()).isEqualTo(PublicationStatus.PUBLISHED);
        }
        for (PackageQuestionSpec question : List.of(
                new PackageQuestionSpec(UUID.randomUUID(), QuestionType.ESSAY, Skill.READING, "{\"correct\":\"A\"}"),
                new PackageQuestionSpec(UUID.randomUUID(), QuestionType.MULTIPLE_CHOICE, Skill.LISTENING, "{\"correct\":\"A\"}"))) {
            ContentPackage pkg = draft();
            when(reader.packageQuestionSpecs(pkg.getVersions().getFirst().getId())).thenReturn(List.of(question));
            assertThatThrownBy(() -> publish(pkg)).isInstanceOf(InvalidContentStateException.class);
        }
    }

    private ContentPackage draft() {
        ContentPackage pkg = new ContentPackage(UUID.randomUUID(), "FINAL", "Final", PackageType.COURSE_TEST, null,
                PublicationStatus.DRAFT, null, null, null, List.of(), null, UUID.randomUUID());
        pkg.addVersion(ContentPackageVersion.create(pkg.getId(), 1, "{}"));
        when(packages.findById(pkg.getId())).thenReturn(Optional.of(pkg));
        return pkg;
    }

    private com.group01.content.application.result.ContentPackageResult publish(ContentPackage pkg) {
        return new PublishContentPackageUseCase(packages, reader).execute(new PublishContentPackageCommand(
                pkg.getId(), pkg.getVersions().getFirst().getId(), UUID.randomUUID()));
    }
}
