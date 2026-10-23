package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.command.AddQuestionVersionCommand;
import com.ieltspath.content.application.command.CreateQuestionCommand;
import com.ieltspath.content.application.command.QuestionDraftRequest;
import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.QuestionType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApproveQuestionDraftUseCaseTest {

    private final ObjectMapper json = new ObjectMapper();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"), ZoneOffset.UTC);
    private final AiDraftRepository drafts = mock(AiDraftRepository.class);
    private final CreateQuestionUseCase create = mock(CreateQuestionUseCase.class);
    private final AddQuestionVersionUseCase addVersion = mock(AddQuestionVersionUseCase.class);
    private final UUID author = UUID.randomUUID();
    private final UUID kpId = UUID.randomUUID();

    @Test
    void approvedItemBecomesADraftQuestionWithAStableReference() throws Exception {
        AiDraft ready = ready("{\"items\":[{\"stem\":\"Q\",\"explanation\":\"E\",\"difficulty\":\"MEDIUM\","
                + "\"options\":[{\"optionKey\":\"A\",\"content\":\"x\",\"sortOrder\":1}],"
                + "\"answerSpec\":{\"type\":\"CHOICE\",\"correct\":\"A\"}}]}");
        when(drafts.findById(ready.getId())).thenReturn(Optional.of(ready));
        when(drafts.save(any(AiDraft.class))).thenAnswer(call -> call.getArgument(0));
        when(create.execute(any(CreateQuestionCommand.class))).thenReturn(new QuestionResult(UUID.randomUUID(),
                QuestionType.MULTIPLE_CHOICE, null, null, PublicationStatus.DRAFT, null, Instant.now(), Instant.now()));

        AiDraft approved = useCase().execute(author, ready.getId(), List.of(0));

        assertEquals(DraftStatus.APPROVED, approved.getStatus());
        verify(create).execute(new CreateQuestionCommand(QuestionType.MULTIPLE_CHOICE, null, null,
                QuestionPurpose.LEARNING, "ai-" + ready.getId() + "-0"));
        verify(addVersion).execute(any(AddQuestionVersionCommand.class));
    }

    @Test
    void indexThatDoesNotExistCreatesNothing() throws Exception {
        AiDraft ready = ready("{\"items\":[]}");
        when(drafts.findById(ready.getId())).thenReturn(Optional.of(ready));

        assertThrows(IllegalArgumentException.class, () -> useCase().execute(author, ready.getId(), List.of(0)));
        verify(create, never()).execute(any());
    }

    @Test
    void draftThatIsNotReadyCannotBeApproved() throws Exception {
        AiDraft queued = new AiDraft(UUID.randomUUID(), DraftKind.QUESTIONS, DraftStatus.QUEUED, author,
                request(), null, null, null, null, Instant.now(), null, null, null, null, null);
        when(drafts.findById(queued.getId())).thenReturn(Optional.of(queued));

        assertThrows(InvalidContentStateException.class, () -> useCase().execute(author, queued.getId(), List.of(0)));
        verify(create, never()).execute(any());
    }

    private ApproveQuestionDraftUseCase useCase() {
        return new ApproveQuestionDraftUseCase(drafts, create, addVersion, json, clock);
    }

    private AiDraft ready(String result) throws Exception {
        return new AiDraft(UUID.randomUUID(), DraftKind.QUESTIONS, DraftStatus.READY, author, request(), result,
                null, "m", "QUESTIONS-v1", Instant.now(), Instant.now(), Instant.now(), null, null, null);
    }

    private String request() throws Exception {
        return json.writeValueAsString(new QuestionDraftRequest(kpId, QuestionType.MULTIPLE_CHOICE, 1,
                QuestionDifficulty.MEDIUM, null));
    }
}
