package com.ieltspath.content.application.service;

import com.ieltspath.content.application.command.AddQuestionVersionCommand;
import com.ieltspath.content.application.command.CreateContentPackageCommand;
import com.ieltspath.content.application.command.CreateQuestionCommand;
import com.ieltspath.content.application.command.SaveContentSectionCommand;
import com.ieltspath.content.application.result.AuthorPackageResult;
import com.ieltspath.content.application.result.AuthorSectionResult;
import com.ieltspath.content.application.result.AuthorVersionResult;
import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.application.usecase.AddQuestionVersionUseCase;
import com.ieltspath.content.application.usecase.AddSectionQuestionUseCase;
import com.ieltspath.content.application.usecase.CreateContentPackageUseCase;
import com.ieltspath.content.application.usecase.CreateContentSectionUseCase;
import com.ieltspath.content.application.usecase.CreateDraftPackageVersionUseCase;
import com.ieltspath.content.application.usecase.CreateQuestionUseCase;
import com.ieltspath.content.application.usecase.SectionMediaUseCases;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.entity.QuestionVersion;
import com.ieltspath.content.domain.exception.InvalidPackageLessonException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.CourseDraftStructure.OptionPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.PracticeSetPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.QuestionPlan;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The order of what a proposed set creates, and what each part is given. The use cases themselves are tested on their own. */
class PracticeSetMaterializerTest {

    private final CreateContentPackageUseCase createPackage = mock(CreateContentPackageUseCase.class);
    private final CreateDraftPackageVersionUseCase createVersion = mock(CreateDraftPackageVersionUseCase.class);
    private final CreateContentSectionUseCase createSection = mock(CreateContentSectionUseCase.class);
    private final SectionMediaUseCases media = mock(SectionMediaUseCases.class);
    private final CreateQuestionUseCase createQuestion = mock(CreateQuestionUseCase.class);
    private final AddQuestionVersionUseCase addVersion = mock(AddQuestionVersionUseCase.class);
    private final AddSectionQuestionUseCase addSectionQuestion = mock(AddSectionQuestionUseCase.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final ContentPackageRepository packages = mock(ContentPackageRepository.class);
    private final PracticeSetMaterializer materializer = new PracticeSetMaterializer(createPackage, createVersion,
            createSection, media, createQuestion, addVersion, addSectionQuestion, questions, packages);

    private final UUID lesson = UUID.randomUUID();
    private final UUID point = UUID.randomUUID();
    private final UUID packageId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();

    private PracticeSetPlan plan() {
        QuestionPlan first = new QuestionPlan(" Which? ", List.of(new OptionPlan("A", " x "), new OptionPlan("B", "y")), "B",
                " Because. ", QuestionDifficulty.HARD);
        QuestionPlan second = new QuestionPlan("Why?", List.of(new OptionPlan("A", "x"), new OptionPlan("B", "y")), "A",
                "Since.", null);
        return new PracticeSetPlan(" Set title ", "  ", "A. The passage.", QuestionType.MULTIPLE_CHOICE, List.of(point),
                List.of(first, second));
    }

    private void stubCreation(List<UUID> questionVersions) {
        AuthorPackageResult pkg = mock(AuthorPackageResult.class);
        when(pkg.id()).thenReturn(packageId);
        when(createPackage.executeAsAuthor(any())).thenReturn(pkg);
        AuthorVersionResult version = mock(AuthorVersionResult.class);
        when(version.id()).thenReturn(versionId);
        when(createVersion.execute(packageId, null)).thenReturn(version);
        AuthorSectionResult section = mock(AuthorSectionResult.class);
        when(section.id()).thenReturn(sectionId);
        when(createSection.execute(any())).thenReturn(section);
        List<QuestionResult> created = new java.util.ArrayList<>();
        for (UUID expectedVersion : questionVersions) {
            UUID questionId = UUID.randomUUID();
            QuestionResult result = mock(QuestionResult.class);
            when(result.id()).thenReturn(questionId);
            Question question = mock(Question.class);
            QuestionVersion questionVersion = mock(QuestionVersion.class);
            when(questionVersion.getId()).thenReturn(expectedVersion);
            when(question.getVersions()).thenReturn(List.of(questionVersion));
            when(questions.findById(questionId)).thenReturn(Optional.of(question));
            created.add(result);
        }
        when(createQuestion.execute(any())).thenReturn(created.get(0), created.subList(1, created.size()).toArray(QuestionResult[]::new));
        when(packages.attachPracticeSetToLesson(packageId, lesson)).thenReturn(true);
    }

    @Test
    void createsADraftPackageSectionPassageAndQuestionsThenAttachesItToTheLesson() {
        UUID firstVersion = UUID.randomUUID();
        UUID secondVersion = UUID.randomUUID();
        stubCreation(List.of(firstVersion, secondVersion));

        UUID created = materializer.create(lesson, "LESSON_01", 1, plan());

        assertEquals(packageId, created);
        verify(addSectionQuestion).execute(packageId, versionId, sectionId, firstVersion, BigDecimal.ONE);
        verify(addSectionQuestion).execute(packageId, versionId, sectionId, secondVersion, BigDecimal.ONE);
        ArgumentCaptor<CreateContentPackageCommand> packageCommand = ArgumentCaptor.forClass(CreateContentPackageCommand.class);
        verify(createPackage).executeAsAuthor(packageCommand.capture());
        assertEquals("PS-LESSON_01-1", packageCommand.getValue().code());
        assertEquals("Set title", packageCommand.getValue().title());
        assertEquals(PackageType.PRACTICE_SET, packageCommand.getValue().packageType());
        ArgumentCaptor<SaveContentSectionCommand> sectionCommand = ArgumentCaptor.forClass(SaveContentSectionCommand.class);
        verify(createSection).execute(sectionCommand.capture());
        assertEquals(Skill.READING, sectionCommand.getValue().skill());
        assertEquals(null, sectionCommand.getValue().instructions());
        InOrder order = inOrder(createPackage, createVersion, createSection, media, packages);
        order.verify(createPackage).executeAsAuthor(any());
        order.verify(createVersion).execute(packageId, null);
        order.verify(createSection).execute(any());
        order.verify(media).setPassage(packageId, versionId, sectionId, "A. The passage.");
        order.verify(packages).attachPracticeSetToLesson(packageId, lesson);
    }

    @Test
    void eachQuestionIsADraftLearningQuestionLinkedToTheSetsKnowledgePoints() {
        UUID questionVersion = UUID.randomUUID();
        stubCreation(List.of(questionVersion));
        PracticeSetPlan single = new PracticeSetPlan("Set", null, "A. Text.", QuestionType.MULTIPLE_CHOICE, List.of(point),
                List.of(plan().questions().get(0)));

        materializer.create(lesson, "L1", 2, single);

        ArgumentCaptor<CreateQuestionCommand> questionCommand = ArgumentCaptor.forClass(CreateQuestionCommand.class);
        verify(createQuestion).execute(questionCommand.capture());
        assertEquals(QuestionType.MULTIPLE_CHOICE, questionCommand.getValue().questionType());
        assertEquals(QuestionPurpose.LEARNING, questionCommand.getValue().purpose());
        ArgumentCaptor<AddQuestionVersionCommand> versionCommand = ArgumentCaptor.forClass(AddQuestionVersionCommand.class);
        verify(addVersion).execute(versionCommand.capture());
        AddQuestionVersionCommand added = versionCommand.getValue();
        assertEquals("Which?", added.stem());
        assertEquals("{\"type\":\"CHOICE\",\"correct\":\"B\"}", added.answerSpecJson());
        assertEquals("Because.", added.explanation());
        assertEquals(QuestionDifficulty.HARD, added.difficulty());
        assertEquals(List.of("A", "B"), added.options().stream().map(option -> option.optionKey()).toList());
        assertEquals("x", added.options().get(0).content());
        assertEquals(List.of(point), added.knowledgePoints().stream().map(link -> link.knowledgePointId()).toList());
        verify(addSectionQuestion).execute(eq(packageId), eq(versionId), eq(sectionId), eq(questionVersion), eq(BigDecimal.ONE));
    }

    @Test
    void aQuestionIsLinkedOnlyToThePointsItNamesAndOtherwiseToAllOfTheSets() {
        UUID otherPoint = UUID.randomUUID();
        stubCreation(List.of(UUID.randomUUID(), UUID.randomUUID()));
        QuestionPlan named = new QuestionPlan("Which?", List.of(new OptionPlan("A", "x"), new OptionPlan("B", "y")), "A",
                "Because.", null, List.of(otherPoint));
        PracticeSetPlan set = new PracticeSetPlan("Set", null, "A. Text.", QuestionType.MULTIPLE_CHOICE,
                List.of(point, otherPoint), List.of(named, plan().questions().get(1)));

        materializer.create(lesson, "L1", 1, set);

        ArgumentCaptor<AddQuestionVersionCommand> versionCommand = ArgumentCaptor.forClass(AddQuestionVersionCommand.class);
        verify(addVersion, org.mockito.Mockito.times(2)).execute(versionCommand.capture());
        assertEquals(List.of(otherPoint), versionCommand.getAllValues().get(0).knowledgePoints().stream()
                .map(link -> link.knowledgePointId()).toList());
        assertEquals(List.of(point, otherPoint), versionCommand.getAllValues().get(1).knowledgePoints().stream()
                .map(link -> link.knowledgePointId()).toList());
    }

    @Test
    void aQuestionWithoutADifficultyIsMedium() {
        stubCreation(List.of(UUID.randomUUID()));
        PracticeSetPlan single = new PracticeSetPlan("Set", null, "A. Text.", QuestionType.MULTIPLE_CHOICE, List.of(point),
                List.of(plan().questions().get(1)));

        materializer.create(lesson, "L1", 1, single);

        ArgumentCaptor<AddQuestionVersionCommand> versionCommand = ArgumentCaptor.forClass(AddQuestionVersionCommand.class);
        verify(addVersion).execute(versionCommand.capture());
        assertEquals(QuestionDifficulty.MEDIUM, versionCommand.getValue().difficulty());
    }

    @Test
    void aSetThatCannotJoinTheLessonFailsTheWholeApproval() {
        stubCreation(List.of(UUID.randomUUID()));
        when(packages.attachPracticeSetToLesson(packageId, lesson)).thenReturn(false);
        PracticeSetPlan single = new PracticeSetPlan("Set", null, "A. Text.", QuestionType.MULTIPLE_CHOICE, List.of(point),
                List.of(plan().questions().get(0)));

        assertThrows(InvalidPackageLessonException.class, () -> materializer.create(lesson, "L1", 1, single));
    }

    @Test
    void aLongLessonCodeStillGivesACodeThatFitsThePackageColumn() {
        stubCreation(List.of(UUID.randomUUID()));
        PracticeSetPlan single = new PracticeSetPlan("Set", null, "A. Text.", QuestionType.MULTIPLE_CHOICE, List.of(point),
                List.of(plan().questions().get(0)));

        materializer.create(lesson, "L".repeat(100), 3, single);

        ArgumentCaptor<CreateContentPackageCommand> packageCommand = ArgumentCaptor.forClass(CreateContentPackageCommand.class);
        verify(createPackage).executeAsAuthor(packageCommand.capture());
        assertEquals(PracticeSetMaterializer.MAX_CODE_PREFIX + 5, packageCommand.getValue().code().length());
    }
}
