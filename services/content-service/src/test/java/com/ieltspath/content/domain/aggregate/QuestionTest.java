package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.entity.QuestionVersion;
import com.ieltspath.content.domain.vo.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionTest {

    @Test
    @DisplayName("Should create question and add version with options")
    void shouldCreateQuestionAndAddVersion() {
        Question question = Question.create(QuestionType.MULTIPLE_CHOICE, Skill.READING, null);

        assertThat(question.getId()).isNotNull();
        assertThat(question.getStatus()).isEqualTo(PublicationStatus.DRAFT);

        List<QuestionOptionPayload> options = List.of(
                new QuestionOptionPayload("A", "Option A", 1),
                new QuestionOptionPayload("B", "Option B", 2)
        );

        QuestionVersion v1 = QuestionVersion.create(
                question.getId(),
                1,
                "What is the capital of France?",
                options,
                "{\"correctKey\":\"A\"}",
                "Paris is the capital.",
                QuestionDifficulty.MEDIUM
        );

        question.addVersion(v1);
        assertThat(question.getVersions()).hasSize(1);
        assertThat(question.getVersions().get(0).getOptions()).hasSize(2);

        question.publishVersion(v1.getId());
        assertThat(question.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(question.getCurrentPublishedVersionId()).isEqualTo(v1.getId());
    }

    @Test
    @DisplayName("Should archive question")
    void shouldArchiveQuestion() {
        Question question = Question.create(QuestionType.MULTIPLE_CHOICE, Skill.READING, null);
        question.archive();

        assertThat(question.getStatus()).isEqualTo(PublicationStatus.ARCHIVED);
        assertThat(question.getUpdatedAt()).isNotNull();
    }

    @Test
    void aQuestionIsForLearningUnlessToldOtherwiseAndKeepsItsPurposeAcrossVersions() {
        assertThat(Question.create(QuestionType.MULTIPLE_CHOICE, Skill.READING, null).getPurpose())
                .isEqualTo(QuestionPurpose.LEARNING);
        Question exam = Question.create(QuestionType.MULTIPLE_CHOICE, Skill.READING, null, QuestionPurpose.EXAM);
        exam.addVersion(QuestionVersion.create(exam.getId(), 1, "Stem", List.of(), "{}", null, QuestionDifficulty.MEDIUM));
        exam.addVersion(QuestionVersion.create(exam.getId(), 2, "Stem v2", List.of(), "{}", null, QuestionDifficulty.MEDIUM));
        assertThat(exam.getPurpose()).isEqualTo(QuestionPurpose.EXAM);
    }

    @Test
    void archivingTheCurrentVersionFallsBackToAnOlderPublishedOne() {
        Question question = Question.create(QuestionType.MULTIPLE_CHOICE, Skill.READING, null);
        QuestionVersion v1 = QuestionVersion.create(question.getId(), 1, "Old", List.of(), "{}", null, QuestionDifficulty.MEDIUM);
        QuestionVersion v2 = QuestionVersion.create(question.getId(), 2, "New", List.of(), "{}", null, QuestionDifficulty.MEDIUM);
        question.addVersion(v1);
        question.addVersion(v2);
        question.publishVersion(v1.getId());
        question.publishVersion(v2.getId());

        question.archiveVersion(v2.getId());

        assertThat(question.getCurrentPublishedVersionId()).isEqualTo(v1.getId());
        assertThat(question.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
    }

    @Test
    void archivingTheLastPublishedVersionLeavesNoCurrentVersion() {
        Question question = Question.create(QuestionType.MULTIPLE_CHOICE, Skill.READING, null);
        QuestionVersion v1 = QuestionVersion.create(question.getId(), 1, "Only", List.of(), "{}", null, QuestionDifficulty.MEDIUM);
        question.addVersion(v1);
        question.publishVersion(v1.getId());

        question.archiveVersion(v1.getId());

        assertThat(question.getCurrentPublishedVersionId()).isNull();
        assertThat(question.getStatus()).isEqualTo(PublicationStatus.DRAFT);
    }
}
