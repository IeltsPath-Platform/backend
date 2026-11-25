package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.command.AddQuestionVersionCommand;
import com.ieltspath.content.application.command.CreateQuestionCommand;
import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.application.port.QuestionStemReader;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.aggregate.Lesson;
import com.ieltspath.content.domain.aggregate.LessonBlock;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.entity.QuestionVersion;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.repository.LessonAuthoringRepository;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.BlockType;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import com.ieltspath.content.domain.vo.LessonStatus;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApproveLessonQuestionDraftUseCaseTest {
    private final ObjectMapper json = new ObjectMapper();
    private final UUID author = UUID.randomUUID();
    private final UUID lessonId = UUID.randomUUID();
    private final UUID pointId = UUID.randomUUID();
    private final UUID blockId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();
    private final AiDraftRepository drafts = mock(AiDraftRepository.class);
    private final LessonAuthoringRepository lessons = mock(LessonAuthoringRepository.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final QuestionStemReader stems = mock(QuestionStemReader.class);
    private final KnowledgePointRepository points = mock(KnowledgePointRepository.class);
    private final CreateQuestionUseCase create = mock(CreateQuestionUseCase.class);
    private final AddQuestionVersionUseCase addVersion = mock(AddQuestionVersionUseCase.class);

    @Test
    void reportsLessonChangedWhenApprovingAfterTheLessonWasPublished() throws Exception {
        AiDraft draft = readyDraft();
        Lesson lesson = lesson(LessonStatus.PUBLISHED, List.of(pointId), List.of());
        lock(draft, lesson);

        assertThatThrownBy(() -> approve().execute(author, draft.getId(), List.of(itemId), 0))
                .isInstanceOfSatisfying(com.ieltspath.content.application.exception.SectionDraftException.class, failure -> {
                    org.assertj.core.api.Assertions.assertThat(failure.status()).isEqualTo(HttpStatus.CONFLICT);
                    org.assertj.core.api.Assertions.assertThat(failure.code()).isEqualTo("LESSON_CHANGED");
                });
        verify(create, never()).execute(any());
    }

    @Test
    void reportsLessonChangedWhenRetryingAnApprovalAfterTheLessonWasPublished() throws Exception {
        AiDraft draft = approvedDraft();
        Lesson lesson = lesson(LessonStatus.PUBLISHED, List.of(pointId), List.of());
        lock(draft, lesson);

        assertThatThrownBy(() -> approve().execute(author, draft.getId(), List.of(itemId), 0))
                .isInstanceOfSatisfying(com.ieltspath.content.application.exception.SectionDraftException.class, failure -> {
                    org.assertj.core.api.Assertions.assertThat(failure.status()).isEqualTo(HttpStatus.CONFLICT);
                    org.assertj.core.api.Assertions.assertThat(failure.code()).isEqualTo("LESSON_CHANGED");
                });
        verify(create, never()).execute(any());
    }

    @Test
    void reportsLessonChangedWhenApprovingAfterTheLessonWasArchived() throws Exception {
        AiDraft draft = readyDraft();
        Lesson lesson = lesson(LessonStatus.ARCHIVED, List.of(pointId), List.of());
        lock(draft, lesson);

        assertThatThrownBy(() -> approve().execute(author, draft.getId(), List.of(itemId), 0))
                .isInstanceOfSatisfying(com.ieltspath.content.application.exception.SectionDraftException.class, failure -> {
                    org.assertj.core.api.Assertions.assertThat(failure.status()).isEqualTo(HttpStatus.CONFLICT);
                    org.assertj.core.api.Assertions.assertThat(failure.code()).isEqualTo("LESSON_CHANGED");
                });
        verify(create, never()).execute(any());
    }

    @Test
    void returnsTheExistingApprovalOnlyForTheSameQuestionSelection() throws Exception {
        AiDraft draft = approvedDraft();
        Lesson lesson = lesson(LessonStatus.DRAFT, List.of(pointId), List.of());
        lock(draft, lesson);

        org.assertj.core.api.Assertions.assertThat(approve().execute(author, draft.getId(), List.of(itemId), 0))
                .isSameAs(draft);
        assertThatThrownBy(() -> approve().execute(author, draft.getId(), List.of(UUID.randomUUID()), 0))
                .isInstanceOfSatisfying(com.ieltspath.content.application.exception.SectionDraftException.class, failure -> {
                    org.assertj.core.api.Assertions.assertThat(failure.status()).isEqualTo(HttpStatus.CONFLICT);
                    org.assertj.core.api.Assertions.assertThat(failure.code()).isEqualTo("DRAFT_ALREADY_REVIEWED");
                });
        verify(create, never()).execute(any());
        verify(addVersion, never()).execute(any());
        verify(create, never()).execute(any());
    }

    @Test
    void refusesApprovalAfterTheKnowledgePointWasRemovedFromTheLesson() throws Exception {
        AiDraft draft = readyDraft();
        Lesson lesson = lesson(LessonStatus.DRAFT, List.of(), List.of());
        lock(draft, lesson);
        KnowledgePoint point = mock(KnowledgePoint.class);
        when(point.getStatus()).thenReturn(ContentStatus.ACTIVE);
        when(points.findById(pointId)).thenReturn(Optional.of(point));

        assertThatThrownBy(() -> approve().execute(author, draft.getId(), List.of(itemId), 0))
                .isInstanceOfSatisfying(com.ieltspath.content.application.exception.SectionDraftException.class, failure -> {
                    org.assertj.core.api.Assertions.assertThat(failure.status()).isEqualTo(HttpStatus.CONFLICT);
                    org.assertj.core.api.Assertions.assertThat(failure.code()).isEqualTo("LESSON_CHANGED");
                });
        verify(create, never()).execute(any());
    }

    @Test
    void offersAConflictWhenTheRequestedExerciseBlockWasRemoved() throws Exception {
        AiDraft draft = readyDraft();
        Lesson lesson = lesson(LessonStatus.DRAFT, List.of(pointId), List.of());
        lock(draft, lesson);
        KnowledgePoint point = mock(KnowledgePoint.class);
        when(point.getStatus()).thenReturn(ContentStatus.ACTIVE);
        when(points.findById(pointId)).thenReturn(Optional.of(point));

        assertThatThrownBy(() -> approve().execute(author, draft.getId(), List.of(itemId), 0))
                .isInstanceOfSatisfying(com.ieltspath.content.application.exception.SectionDraftException.class, failure -> {
                    org.assertj.core.api.Assertions.assertThat(failure.status()).isEqualTo(HttpStatus.CONFLICT);
                    org.assertj.core.api.Assertions.assertThat(failure.code()).isEqualTo("BLOCK_MISSING");
                });
        verify(create, never()).execute(any());
    }

    @Test
    void locksLessonBeforeDraftAndUsesTopicSkillBeforeKnowledgePointSkill() throws Exception {
        AiDraft draft = readyDraft();
        LessonBlock exercise = new LessonBlock(blockId, lessonId, BlockType.EXERCISE, 0, null, null, List.of(), List.of(UUID.randomUUID()), List.of());
        Lesson lesson = lesson(LessonStatus.DRAFT, List.of(pointId), List.of(exercise));
        lock(draft, lesson);
        KnowledgePoint point = mock(KnowledgePoint.class);
        when(point.getStatus()).thenReturn(ContentStatus.ACTIVE);
        when(points.findById(pointId)).thenReturn(Optional.of(point));
        when(lessons.courseTopicSkillForLesson(lessonId)).thenReturn(Optional.of(Skill.WRITING));
        when(lessons.knowledgePointSkills(List.of(pointId))).thenReturn(Map.of(pointId, Skill.LISTENING));
        UUID questionId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        when(create.execute(any(CreateQuestionCommand.class))).thenReturn(new QuestionResult(questionId,
                QuestionType.MULTIPLE_CHOICE, Skill.WRITING, null, PublicationStatus.DRAFT, null, Instant.now(), Instant.now()));
        Question saved = mock(Question.class);
        QuestionVersion version = mock(QuestionVersion.class);
        when(version.getId()).thenReturn(versionId);
        when(saved.getVersions()).thenReturn(new ArrayList<>(List.of(version)));
        when(questions.findById(questionId)).thenReturn(Optional.of(saved));
        when(lessons.questionVersionsOwnedElsewhere(blockId, List.of(versionId))).thenReturn(List.of());
        when(lessons.save(lesson)).thenReturn(lesson);
        when(drafts.save(draft)).thenReturn(draft);

        approve().execute(author, draft.getId(), List.of(itemId), 0);

        var lockOrder = inOrder(lessons, drafts);
        lockOrder.verify(lessons).lockAndFindById(lessonId);
        lockOrder.verify(drafts).findByIdForUpdate(draft.getId());
        verify(create).execute(new CreateQuestionCommand(QuestionType.MULTIPLE_CHOICE, Skill.WRITING, null,
                QuestionPurpose.LEARNING, "ai-" + draft.getId() + "-" + itemId));
        verify(addVersion).execute(any(AddQuestionVersionCommand.class));
    }

    @Test
    void leavesSkillEmptyWhenTopicAndKnowledgePointHaveNoSpecificSkill() throws Exception {
        AiDraft draft = readyDraft();
        LessonBlock exercise = new LessonBlock(blockId, lessonId, BlockType.EXERCISE, 0, null, null, List.of(), List.of(UUID.randomUUID()), List.of());
        Lesson lesson = lesson(LessonStatus.DRAFT, List.of(pointId), List.of(exercise));
        lock(draft, lesson);
        KnowledgePoint point = mock(KnowledgePoint.class);
        when(point.getStatus()).thenReturn(ContentStatus.ACTIVE);
        when(points.findById(pointId)).thenReturn(Optional.of(point));
        when(lessons.courseTopicSkillForLesson(lessonId)).thenReturn(Optional.of(Skill.ALL));
        when(lessons.knowledgePointSkills(List.of(pointId))).thenReturn(Map.of(pointId, Skill.ALL));
        UUID questionId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        when(create.execute(any(CreateQuestionCommand.class))).thenReturn(new QuestionResult(questionId,
                QuestionType.MULTIPLE_CHOICE, null, null, PublicationStatus.DRAFT, null, Instant.now(), Instant.now()));
        Question saved = mock(Question.class);
        QuestionVersion version = mock(QuestionVersion.class);
        when(version.getId()).thenReturn(versionId);
        when(saved.getVersions()).thenReturn(new ArrayList<>(List.of(version)));
        when(questions.findById(questionId)).thenReturn(Optional.of(saved));
        when(lessons.questionVersionsOwnedElsewhere(blockId, List.of(versionId))).thenReturn(List.of());
        when(lessons.save(lesson)).thenReturn(lesson);
        when(drafts.save(draft)).thenReturn(draft);

        approve().execute(author, draft.getId(), List.of(itemId), 0);

        verify(create).execute(new CreateQuestionCommand(QuestionType.MULTIPLE_CHOICE, null, null,
                QuestionPurpose.LEARNING, "ai-" + draft.getId() + "-" + itemId));
    }

    private void lock(AiDraft draft, Lesson lesson) {
        when(drafts.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(drafts.findByIdForUpdate(draft.getId())).thenReturn(Optional.of(draft));
        when(lessons.lockAndFindById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessons.courseTopicSkillForLesson(lessonId)).thenReturn(Optional.empty());
        when(stems.stemsOf(pointId)).thenReturn(List.of());
    }

    private ApproveQuestionDraftUseCase approve() {
        return new ApproveQuestionDraftUseCase(drafts, create, addVersion, json,
                Clock.fixed(Instant.parse("2026-11-25T00:00:00Z"), ZoneOffset.UTC), lessons, questions, stems, points);
    }

    private Lesson lesson(LessonStatus status, List<UUID> points, List<com.ieltspath.content.domain.aggregate.LessonBlock> blocks) {
        return new Lesson(lessonId, UUID.randomUUID(), "L-1", "Lesson", null, null, 10, 0, status,
                Instant.now(), blocks, points);
    }

    private AiDraft readyDraft() throws Exception {
        String request = json.writeValueAsString(new com.ieltspath.content.application.command.QuestionDraftRequest(
                pointId, QuestionType.MULTIPLE_CHOICE, 1, QuestionDifficulty.MEDIUM, null, lessonId, blockId));
        return new AiDraft(UUID.randomUUID(), DraftKind.QUESTIONS, DraftStatus.READY, author, request,
                "{\"revision\":0,\"items\":[{\"itemId\":\"" + itemId + "\",\"stem\":\"A unique stem\",\"options\":[],\"answerSpec\":{\"type\":\"CHOICE\",\"correct\":\"A\"},\"explanation\":\"Reason\"}],\"rejected\":[]}",
                null, "test", "QUESTIONS-v3", Instant.now(), null, Instant.now(), null, null, null);
    }

    private AiDraft approvedDraft() throws Exception {
        String request = json.writeValueAsString(new com.ieltspath.content.application.command.QuestionDraftRequest(
                pointId, QuestionType.MULTIPLE_CHOICE, 1, QuestionDifficulty.MEDIUM, null, lessonId, blockId));
        return new AiDraft(UUID.randomUUID(), DraftKind.QUESTIONS, DraftStatus.APPROVED, author, request,
                "{\"revision\":0,\"items\":[{\"itemId\":\"" + itemId + "\",\"stem\":\"A unique stem\"}],"
                        + "\"approved\":{\"" + itemId + "\":\"" + UUID.randomUUID() + "\"}}",
                null, "test", "QUESTIONS-v3", Instant.now(), Instant.now(), Instant.now(), author, Instant.now(), null);
    }
}
