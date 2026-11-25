package com.ieltspath.content.domain.entity;

import com.ieltspath.content.domain.exception.QuestionAlreadyInVersionException;
import com.ieltspath.content.domain.exception.SectionQuestionNotFoundException;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SectionQuestionOrderTest {

    @Test
    void newQuestionsGoAfterTheLastOne() {
        ContentSection section = section();
        section.addQuestion(SectionQuestion.create(section.getId(), UUID.randomUUID(), 1, BigDecimal.ONE));
        section.addQuestion(SectionQuestion.create(section.getId(), UUID.randomUUID(), 2, BigDecimal.ONE));

        assertEquals(3, section.nextQuestionSortOrder());
    }

    @Test
    void aWritingSectionKeepsItsOneQuestion() {
        ContentSection writing = ContentSection.create(UUID.randomUUID(), "Task 1", Skill.WRITING, 1, null, null);
        SectionQuestion first = SectionQuestion.create(writing.getId(), UUID.randomUUID(), 1, BigDecimal.ONE);
        writing.addQuestion(first);

        assertThrows(IllegalArgumentException.class,
                () -> writing.addQuestion(SectionQuestion.create(writing.getId(), UUID.randomUUID(), 2, BigDecimal.ONE)));
        assertThrows(IllegalArgumentException.class, () -> writing.removeQuestion(first.getId()));
        assertEquals(1, writing.getQuestions().size());
    }

    @Test
    void aWritingSectionThatAlreadyHoldsSeveralCanBeTrimmedDownToOne() {
        UUID version = UUID.randomUUID();
        SectionQuestion first = SectionQuestion.create(UUID.randomUUID(), UUID.randomUUID(), 1, BigDecimal.ONE);
        SectionQuestion second = SectionQuestion.create(first.getSectionId(), UUID.randomUUID(), 2, BigDecimal.ONE);
        ContentSection writing = new ContentSection(first.getSectionId(), version, "Task 1", Skill.WRITING, 1, null, null,
                null, null, List.of(first, second));

        writing.removeQuestion(second.getId());

        assertEquals(1, writing.getQuestions().size());
        assertThrows(IllegalArgumentException.class, () -> writing.removeQuestion(first.getId()));
    }

    @Test
    void removalRenumbersTheRestInTheirOwnOrder() {
        ContentSection section = section();
        SectionQuestion first = add(section, 1);
        SectionQuestion second = add(section, 2);
        SectionQuestion third = add(section, 3);

        section.removeQuestion(second.getId());

        assertEquals(1, first.getSortOrder());
        assertEquals(2, third.getSortOrder());
        assertThrows(SectionQuestionNotFoundException.class, () -> section.requireQuestion(second.getId()));
    }

    @Test
    void reorderNeedsEveryQuestionExactlyOnce() {
        ContentSection section = section();
        SectionQuestion a = add(section, 1);
        SectionQuestion b = add(section, 2);

        section.reorderQuestions(List.of(b.getId(), a.getId()));
        assertEquals(1, b.getSortOrder());
        assertEquals(2, a.getSortOrder());

        assertThrows(IllegalArgumentException.class, () -> section.reorderQuestions(List.of(a.getId())));
        assertThrows(IllegalArgumentException.class, () -> section.reorderQuestions(List.of(a.getId(), a.getId())));
    }

    @Test
    void questionVersionMayAppearOnceInTheVersion() {
        ContentPackageVersion version = ContentPackageVersion.create(UUID.randomUUID(), 1, "{}");
        ContentSection section = ContentSection.create(version.getId(), "Passage", null, 1, null, null);
        version.addSection(section);
        UUID questionVersion = UUID.randomUUID();
        section.addQuestion(SectionQuestion.create(section.getId(), questionVersion, 1, BigDecimal.ONE));

        assertThrows(QuestionAlreadyInVersionException.class,
                () -> version.requireQuestionNotInVersion(questionVersion));
        version.requireQuestionNotInVersion(UUID.randomUUID());
    }

    private static ContentSection section() {
        return ContentSection.create(UUID.randomUUID(), "Passage", null, 1, null, null);
    }

    private static SectionQuestion add(ContentSection section, int order) {
        SectionQuestion question = SectionQuestion.create(section.getId(), UUID.randomUUID(), order, BigDecimal.ONE);
        section.addQuestion(question);
        return question;
    }
}
