package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.domain.repository.LessonAuthoringRepository;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListQuestionsWithIncludesUseCaseTest {
    @Mock ListQuestionsUseCase listQuestions;
    @Mock QuestionRepository questions;
    @Mock LessonAuthoringRepository lessonAuthoring;

    @Test
    void enrichesOnePageWithOwnershipAndBatchedPreviews() {
        UUID versionId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID exceptBlock = UUID.randomUUID();
        String longStem = "first word ".repeat(25);
        QuestionResult question = new QuestionResult(questionId, QuestionType.MULTIPLE_CHOICE, Skill.READING,
                null, PublicationStatus.PUBLISHED, versionId, Instant.now(), Instant.now(), QuestionPurpose.LEARNING);
        when(listQuestions.execute(Skill.READING, QuestionPurpose.LEARNING, PublicationStatus.PUBLISHED,
                "Q-1", 0, 25)).thenReturn(new ListQuestionsUseCase.Page(1, 0, 25, List.of(question)));
        when(lessonAuthoring.questionVersionsOwnedElsewhere(exceptBlock, List.of(versionId))).thenReturn(List.of(versionId));
        when(questions.findVersionStems(List.of(versionId))).thenReturn(List.of(new QuestionRepository.VersionStem(versionId, longStem)));
        when(questions.findVersionKnowledgePoints(List.of(versionId))).thenReturn(List.of(
                new QuestionRepository.VersionKnowledgePoint(versionId, "R1", "Một", 5),
                new QuestionRepository.VersionKnowledgePoint(versionId, "R2", "Hai", 5),
                new QuestionRepository.VersionKnowledgePoint(versionId, "R3", "Ba", 5),
                new QuestionRepository.VersionKnowledgePoint(versionId, "R4", "Bốn", 5)));

        var page = new ListQuestionsWithIncludesUseCase(listQuestions, questions, lessonAuthoring)
                .execute(Skill.READING, QuestionPurpose.LEARNING, PublicationStatus.PUBLISHED,
                        "Q-1", 0, 25, Set.of("ownership", "preview"), false, exceptBlock, null);

        QuestionResult result = page.items().getFirst();
        assertThat(result.ownedElsewhere()).isTrue();
        assertThat(result.stemExcerpt()).hasSizeLessThanOrEqualTo(160).endsWith("…");
        assertThat(result.knowledgePoints()).extracting("code").containsExactly("R1", "R2", "R3", "R4");
        assertThat(result.knowledgePointTotal()).isEqualTo(5);
        verify(lessonAuthoring).questionVersionsOwnedElsewhere(exceptBlock, List.of(versionId));
        verify(questions).findVersionStems(List.of(versionId));
        verify(questions).findVersionKnowledgePoints(List.of(versionId));
    }

    @Test
    void leavesExistingCatalogShapeUnchangedWhenNoIncludeWasRequested() {
        QuestionResult question = new QuestionResult(UUID.randomUUID(), QuestionType.MULTIPLE_CHOICE, Skill.READING,
                null, PublicationStatus.PUBLISHED, null, Instant.now(), Instant.now(), QuestionPurpose.LEARNING);
        when(listQuestions.execute(null, null, null, null, 0, 25))
                .thenReturn(new ListQuestionsUseCase.Page(1, 0, 25, List.of(question)));

        var page = new ListQuestionsWithIncludesUseCase(listQuestions, questions, lessonAuthoring)
                .execute(null, null, null, null, 0, 25, Set.of(), false, null, null);

        assertThat(page.items().getFirst()).isSameAs(question);
    }
}
