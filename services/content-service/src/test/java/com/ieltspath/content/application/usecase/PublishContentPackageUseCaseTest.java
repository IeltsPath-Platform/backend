package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.PublishContentPackageCommand;
import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.ContentPackageResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.exception.InvalidPackageLessonException;
import com.ieltspath.content.domain.exception.QuestionAlreadyUsedException;
import com.ieltspath.content.domain.exception.QuestionPurposeMismatchException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.QuestionUsageConflict;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class PublishContentPackageUseCaseTest {

    @Mock
    private ContentPackageRepository contentPackageRepository;

    @Mock
    private LearningContentReader lessons;

    @InjectMocks
    private PublishContentPackageUseCase publishContentPackageUseCase;

    private ContentPackage contentPackage;
    private ContentPackageVersion version;
    private UUID packageId;
    private UUID versionId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        packageId = UUID.randomUUID();
        userId = UUID.randomUUID();
        contentPackage = ContentPackage.create("PKG_01", "Mock Test", PackageType.MOCK_TEST, null);
        version = ContentPackageVersion.create(contentPackage.getId(), 1, "{}");
        versionId = version.getId();
        contentPackage.addVersion(version);
    }

    @Test
    @DisplayName("Should publish content package version successfully")
    void shouldPublishPackageVersionSuccessfully() {
        when(contentPackageRepository.findById(contentPackage.getId())).thenReturn(Optional.of(contentPackage));
        when(contentPackageRepository.save(any(ContentPackage.class))).thenAnswer(inv -> inv.getArgument(0));

        PublishContentPackageCommand command = new PublishContentPackageCommand(contentPackage.getId(), versionId, userId);
        ContentPackageResult result = publishContentPackageUseCase.execute(command);

        assertThat(result.status()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(result.currentPublishedVersionId()).isEqualTo(versionId);
        verify(contentPackageRepository).save(contentPackage);
    }

    @Test
    @DisplayName("Should throw exception if package not found")
    void shouldThrowWhenPackageNotFound() {
        UUID nonExistent = UUID.randomUUID();
        when(contentPackageRepository.findById(nonExistent)).thenReturn(Optional.empty());

        PublishContentPackageCommand command = new PublishContentPackageCommand(nonExistent, versionId, userId);

        assertThatThrownBy(() -> publishContentPackageUseCase.execute(command))
                .isInstanceOf(ContentPackageNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception if version does not belong to package")
    void shouldThrowWhenVersionDoesNotBelongToPackage() {
        UUID wrongVersionId = UUID.randomUUID();
        when(contentPackageRepository.findById(contentPackage.getId())).thenReturn(Optional.of(contentPackage));

        PublishContentPackageCommand command = new PublishContentPackageCommand(contentPackage.getId(), wrongVersionId, userId);

        assertThatThrownBy(() -> publishContentPackageUseCase.execute(command))
                .isInstanceOf(InvalidContentStateException.class);
    }

    @Test
    void questionsOwnedElsewherePreventPublishingWithoutChangingTheDraft() {
        UUID questionId = UUID.randomUUID();
        when(contentPackageRepository.findById(contentPackage.getId())).thenReturn(Optional.of(contentPackage));
        when(lessons.questionsUsedElsewhere(versionId)).thenReturn(List.of(
                new QuestionUsageConflict(questionId, QuestionUsageConflict.OwnerType.LESSON, "L1")));

        assertThatThrownBy(() -> publishContentPackageUseCase.execute(
                new PublishContentPackageCommand(contentPackage.getId(), versionId, userId)))
                .isInstanceOf(QuestionAlreadyUsedException.class)
                .hasMessageContaining(questionId.toString()).hasMessageContaining("LESSON L1");
        verify(contentPackageRepository, never()).save(any());
        assertThat(contentPackage.getStatus()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(version.getStatus()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(contentPackage.getCurrentPublishedVersionId()).isNull();
    }

    @Test
    void aLessonsPracticeSetCannotPublishQuestionsOfAnotherSkill() {
        UUID lessonId = UUID.randomUUID();
        ContentPackage practice = ContentPackage.create("PS-NEW", "Practice", PackageType.PRACTICE_SET, null, lessonId);
        ContentPackageVersion practiceVersion = ContentPackageVersion.create(practice.getId(), 1, "{}");
        practice.addVersion(practiceVersion);
        when(contentPackageRepository.findById(practice.getId())).thenReturn(Optional.of(practice));
        when(lessons.packageVersionLeavesLessonSkills(practiceVersion.getId(), lessonId)).thenReturn(true);

        assertThatThrownBy(() -> publishContentPackageUseCase.execute(
                new PublishContentPackageCommand(practice.getId(), practiceVersion.getId(), userId)))
                .isInstanceOf(InvalidPackageLessonException.class);
        verify(contentPackageRepository, never()).save(any());
        assertThat(practice.getStatus()).isEqualTo(PublicationStatus.DRAFT);
    }

    @ParameterizedTest
    @CsvSource({"PRACTICE_SET,LEARNING", "TOPIC_TEST,LEARNING", "MOCK_TEST,EXAM", "PLACEMENT_TEST,EXAM"})
    void questionsMustMatchThePackagesPurposeBeforePublishing(PackageType type, QuestionPurpose requiredPurpose) {
        ContentPackage pkg = ContentPackage.create("PURPOSE-CHECK", "Purpose", type, null);
        ContentPackageVersion draft = ContentPackageVersion.create(pkg.getId(), 1, "{}");
        pkg.addVersion(draft);
        UUID questionId = UUID.randomUUID();
        when(contentPackageRepository.findById(pkg.getId())).thenReturn(Optional.of(pkg));
        when(lessons.questionsWithWrongPurpose(draft.getId(), requiredPurpose)).thenReturn(List.of(questionId));

        assertThatThrownBy(() -> publishContentPackageUseCase.execute(
                new PublishContentPackageCommand(pkg.getId(), draft.getId(), userId)))
                .isInstanceOf(QuestionPurposeMismatchException.class)
                .hasMessageContaining(requiredPurpose.name()).hasMessageContaining(questionId.toString());
        verify(contentPackageRepository, never()).save(any());
        assertThat(pkg.getStatus()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(draft.getStatus()).isEqualTo(PublicationStatus.DRAFT);
        InOrder checks = inOrder(lessons);
        checks.verify(lessons).lockQuestionsForPublishing(draft.getId());
        checks.verify(lessons).questionsUsedElsewhere(draft.getId());
        checks.verify(lessons).questionsWithWrongPurpose(draft.getId(), requiredPurpose);
    }
}
