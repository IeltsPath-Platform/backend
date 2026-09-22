package com.group01.content.domain.aggregate;

import com.group01.content.domain.entity.QuestionVersion;
import com.group01.content.domain.vo.AccessLevel;
import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.domain.vo.QuestionDifficulty;
import com.group01.content.domain.vo.QuestionOptionPayload;
import com.group01.content.domain.vo.QuestionType;
import com.group01.content.domain.vo.Skill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionTest {

    @Test
    @DisplayName("Should create question and add version with options")
    void shouldCreateQuestionAndAddVersion() {
        Question question = Question.create(QuestionType.MULTIPLE_CHOICE, Skill.READING, AccessLevel.FREE);

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
}

