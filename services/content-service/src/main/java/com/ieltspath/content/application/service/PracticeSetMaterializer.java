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
import com.ieltspath.content.domain.exception.InvalidPackageLessonException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.CourseDraftStructure.OptionPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.PracticeSetPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.QuestionPlan;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionOptionPayload;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.Skill;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Creates the practice set a model wrote for a lesson, through the same use cases the authoring screens use: a draft
 * package with one reading section, its passage, and the questions as draft learning questions linked to the set's
 * knowledge points. Nothing is published, so the author still reads and publishes the set in the package screens. It
 * runs inside the caller's transaction.
 */
@Service
public class PracticeSetMaterializer {

    static final int MAX_CODE_PREFIX = 90;

    private final CreateContentPackageUseCase createPackage;
    private final CreateDraftPackageVersionUseCase createVersion;
    private final CreateContentSectionUseCase createSection;
    private final SectionMediaUseCases media;
    private final CreateQuestionUseCase createQuestion;
    private final AddQuestionVersionUseCase addQuestionVersion;
    private final AddSectionQuestionUseCase addSectionQuestion;
    private final QuestionRepository questions;
    private final ContentPackageRepository packages;

    public PracticeSetMaterializer(CreateContentPackageUseCase createPackage,
                                   CreateDraftPackageVersionUseCase createVersion,
                                   CreateContentSectionUseCase createSection, SectionMediaUseCases media,
                                   CreateQuestionUseCase createQuestion, AddQuestionVersionUseCase addQuestionVersion,
                                   AddSectionQuestionUseCase addSectionQuestion, QuestionRepository questions,
                                   ContentPackageRepository packages) {
        this.createPackage = createPackage;
        this.createVersion = createVersion;
        this.createSection = createSection;
        this.media = media;
        this.createQuestion = createQuestion;
        this.addQuestionVersion = addQuestionVersion;
        this.addSectionQuestion = addSectionQuestion;
        this.questions = questions;
        this.packages = packages;
    }

    /**
     * @param ordinal the position of the set among the lesson's new sets, from 1; it makes the package code unique
     * @return the id of the draft package, now attached to the lesson
     */
    @Transactional
    public UUID create(UUID lessonId, String lessonCode, int ordinal, PracticeSetPlan plan) {
        String code = "PS-" + clip(lessonCode, MAX_CODE_PREFIX) + "-" + ordinal;
        AuthorPackageResult pkg = createPackage.executeAsAuthor(new CreateContentPackageCommand(code,
                clip(plan.title().trim(), 255), PackageType.PRACTICE_SET, null, null));
        AuthorVersionResult version = createVersion.execute(pkg.id(), null);
        AuthorSectionResult section = createSection.execute(new SaveContentSectionCommand(pkg.id(), version.id(), null,
                clip(plan.title().trim(), 255), Skill.READING, 1, null, blankToNull(plan.instructions()), null, null));
        media.setPassage(pkg.id(), version.id(), section.id(), plan.passage().trim());
        for (QuestionPlan question : plan.questions()) {
            UUID questionVersion = createQuestion(plan, question);
            addSectionQuestion.execute(pkg.id(), version.id(), section.id(), questionVersion, BigDecimal.ONE);
        }
        if (!packages.attachPracticeSetToLesson(pkg.id(), lessonId)) {
            throw new InvalidPackageLessonException("Package " + pkg.id() + " is not a practice set that is free to join a lesson");
        }
        return pkg.id();
    }

    /** A draft question with one version, linked to the set's knowledge points. @return the version's id */
    private UUID createQuestion(PracticeSetPlan plan, QuestionPlan question) {
        QuestionResult created = createQuestion.execute(new CreateQuestionCommand(plan.questionType(), Skill.READING,
                null, QuestionPurpose.LEARNING));
        List<QuestionOptionPayload> options = new ArrayList<>();
        for (OptionPlan option : question.options()) {
            options.add(new QuestionOptionPayload(option.optionKey(), option.content().trim(), options.size() + 1));
        }
        List<UUID> tested = question.knowledgePointIds().isEmpty() ? plan.knowledgePointIds() : question.knowledgePointIds();
        List<AddQuestionVersionCommand.KnowledgePointInput> points = tested.stream()
                .map(point -> new AddQuestionVersionCommand.KnowledgePointInput(null, point, BigDecimal.ONE)).toList();
        addQuestionVersion.execute(new AddQuestionVersionCommand(created.id(), 1, question.stem().trim(), options,
                "{\"type\":\"CHOICE\",\"correct\":\"" + question.correctKey() + "\"}", question.explanation().trim(),
                null, question.difficulty() == null ? QuestionDifficulty.MEDIUM : question.difficulty(), points));
        return questions.findById(created.id()).orElseThrow().getVersions().get(0).getId();
    }

    private static String clip(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
