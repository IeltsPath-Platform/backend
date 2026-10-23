package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.command.QuestionDraftItemPatch;
import com.ieltspath.content.application.command.QuestionDraftRequest;
import com.ieltspath.content.application.port.QuestionStemReader;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.aggregate.ErrorType;
import com.ieltspath.content.domain.exception.AiDraftNotFoundException;
import com.ieltspath.content.domain.exception.DraftItemNotFoundException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.exception.InvalidDraftItemException;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EditDraftItemUseCaseTest {

    private final ObjectMapper json = new ObjectMapper();
    private final AiDraftRepository drafts = mock(AiDraftRepository.class);
    private final ErrorTypeRepository errorTypes = mock(ErrorTypeRepository.class);
    private final UUID author = UUID.randomUUID();
    private final UUID kpId = UUID.randomUUID();
    private List<String> existingStems = List.of("The stem already in the bank");

    private final QuestionStemReader stems = id -> existingStems;

    @BeforeEach
    void setUp() {
        ErrorType trap = mock(ErrorType.class);
        when(trap.getCode()).thenReturn("PARAPHRASE_TRAP");
        when(errorTypes.findApprovedActive()).thenReturn(List.of(trap));
        when(drafts.save(any(AiDraft.class))).thenAnswer(call -> call.getArgument(0));
    }

    private EditDraftItemUseCase useCase() {
        return new EditDraftItemUseCase(drafts, errorTypes, stems, json);
    }

    private AiDraft draft(QuestionType type, DraftStatus status, String result) throws Exception {
        String request = json.writeValueAsString(new QuestionDraftRequest(kpId, type, 3, QuestionDifficulty.MEDIUM,
                null));
        AiDraft draft = new AiDraft(UUID.randomUUID(), DraftKind.QUESTIONS, status, author, request, result, null, "m",
                "QUESTIONS-v1", Instant.now(), Instant.now(), Instant.now(), null, null, null);
        when(drafts.findById(draft.getId())).thenReturn(Optional.of(draft));
        return draft;
    }

    private static final String CHOICE_ITEMS = "{\"items\":["
            + "{\"stem\":\"First\",\"explanation\":\"E1\",\"difficulty\":\"MEDIUM\",\"options\":["
            + "{\"optionKey\":\"A\",\"content\":\"x\",\"sortOrder\":1},{\"optionKey\":\"B\",\"content\":\"y\",\"sortOrder\":2}],"
            + "\"answerSpec\":{\"type\":\"CHOICE\",\"correct\":\"A\",\"distractors\":{\"B\":\"PARAPHRASE_TRAP\"}}},"
            + "{\"stem\":\"Second\",\"explanation\":\"E2\",\"difficulty\":\"MEDIUM\",\"options\":["
            + "{\"optionKey\":\"A\",\"content\":\"p\",\"sortOrder\":1},{\"optionKey\":\"B\",\"content\":\"q\",\"sortOrder\":2}],"
            + "\"answerSpec\":{\"type\":\"CHOICE\",\"correct\":\"B\"}}],"
            + "\"rejected\":[{\"index\":2,\"reason\":\"BAD_OPTIONS\"}]}";

    private JsonNode resultOf(AiDraft draft) throws Exception {
        return json.readTree(draft.getResultJson());
    }

    @Test
    void anEditChangesOnlyTheFieldsGivenAndRebuildsTheAnswerSpec() throws Exception {
        AiDraft draft = draft(QuestionType.MULTIPLE_CHOICE, DraftStatus.READY, CHOICE_ITEMS);

        useCase().edit(author, draft.getId(), 0, new QuestionDraftItemPatch("First, reworded", null, null, "B",
                Map.of("A", "PARAPHRASE_TRAP"), null));

        JsonNode first = resultOf(draft).path("items").get(0);
        assertEquals("First, reworded", first.path("stem").asText());
        assertEquals("E1", first.path("explanation").asText());
        assertEquals("MEDIUM", first.path("difficulty").asText());
        assertEquals("B", first.path("answerSpec").path("correct").asText());
        assertEquals("PARAPHRASE_TRAP", first.path("answerSpec").path("distractors").path("A").asText());
        assertEquals(2, first.path("options").size());
        assertEquals("Second", resultOf(draft).path("items").get(1).path("stem").asText());
        assertEquals("BAD_OPTIONS", resultOf(draft).path("rejected").get(0).path("reason").asText());
        verify(drafts).save(draft);
    }

    @Test
    void newOptionsReplaceTheOldOnesAndAreNumberedAgain() throws Exception {
        AiDraft draft = draft(QuestionType.MULTIPLE_CHOICE, DraftStatus.READY, CHOICE_ITEMS);

        useCase().edit(author, draft.getId(), 1, new QuestionDraftItemPatch(null, null, List.of(
                new QuestionDraftItemPatch.Option("A", "p"), new QuestionDraftItemPatch.Option("B", "q"),
                new QuestionDraftItemPatch.Option("C", "r")), "C", null, null));

        JsonNode second = resultOf(draft).path("items").get(1);
        assertEquals(3, second.path("options").size());
        assertEquals(3, second.path("options").get(2).path("sortOrder").asInt());
        assertEquals("C", second.path("answerSpec").path("correct").asText());
    }

    @Test
    void aTypedItemTakesNewAcceptedAnswers() throws Exception {
        AiDraft draft = draft(QuestionType.FILL_IN_BLANK, DraftStatus.READY, "{\"items\":[{\"stem\":\"Fill\","
                + "\"explanation\":\"E\",\"difficulty\":\"MEDIUM\",\"options\":[],"
                + "\"answerSpec\":{\"type\":\"FILL\",\"accepted\":[\"colour\"]}}]}");

        useCase().edit(author, draft.getId(), 0, new QuestionDraftItemPatch(null, null, null, null, null,
                List.of("colour", "color")));

        assertEquals(List.of("colour", "color"), json.convertValue(
                resultOf(draft).path("items").get(0).path("answerSpec").path("accepted"), List.class));
    }

    @Test
    void anEditThatBreaksARuleIsRefusedWithItsReasonAndSavesNothing() throws Exception {
        AiDraft draft = draft(QuestionType.MULTIPLE_CHOICE, DraftStatus.READY, CHOICE_ITEMS);

        assertEquals("CORRECT_NOT_AN_OPTION", assertThrows(InvalidDraftItemException.class, () -> useCase().edit(author,
                draft.getId(), 0, new QuestionDraftItemPatch(null, null, null, "Z", null, null))).getReason());
        assertEquals("UNKNOWN_ERROR_CODE", assertThrows(InvalidDraftItemException.class, () -> useCase().edit(author,
                draft.getId(), 0, new QuestionDraftItemPatch(null, null, null, null, Map.of("B", "MADE_UP"),
                        null))).getReason());
        assertEquals("NO_EXPLANATION", assertThrows(InvalidDraftItemException.class, () -> useCase().edit(author,
                draft.getId(), 0, new QuestionDraftItemPatch(null, " ", null, null, null, null))).getReason());
        verify(drafts, never()).save(any());
        assertEquals("First", resultOf(draft).path("items").get(0).path("stem").asText());
    }

    @Test
    void aStemOfTheBankOrOfAnotherItemIsADuplicateButTheItemsOwnStemIsNot() throws Exception {
        AiDraft draft = draft(QuestionType.MULTIPLE_CHOICE, DraftStatus.READY, CHOICE_ITEMS);
        existingStems = List.of("The stem already in the bank");

        assertEquals("DUPLICATE_STEM", assertThrows(InvalidDraftItemException.class, () -> useCase().edit(author,
                draft.getId(), 0, new QuestionDraftItemPatch("the stem  ALREADY in the bank", null, null, null, null,
                        null))).getReason());
        assertEquals("DUPLICATE_STEM", assertThrows(InvalidDraftItemException.class, () -> useCase().edit(author,
                draft.getId(), 0, new QuestionDraftItemPatch(" second ", null, null, null, null, null))).getReason());

        useCase().edit(author, draft.getId(), 0, new QuestionDraftItemPatch("First", "Better explanation", null, null,
                null, null));
        assertEquals("Better explanation", resultOf(draft).path("items").get(0).path("explanation").asText());
    }

    @Test
    void removingAnItemClosesTheGap() throws Exception {
        AiDraft draft = draft(QuestionType.MULTIPLE_CHOICE, DraftStatus.READY, CHOICE_ITEMS);

        useCase().remove(author, draft.getId(), 0);

        assertEquals(1, resultOf(draft).path("items").size());
        assertEquals("Second", resultOf(draft).path("items").get(0).path("stem").asText());
        assertEquals(1, resultOf(draft).path("rejected").size());
    }

    @Test
    void aPositionThatDoesNotExistIsNotFound() throws Exception {
        AiDraft draft = draft(QuestionType.MULTIPLE_CHOICE, DraftStatus.READY, CHOICE_ITEMS);

        assertThrows(DraftItemNotFoundException.class, () -> useCase().remove(author, draft.getId(), 2));
        assertThrows(DraftItemNotFoundException.class, () -> useCase().remove(author, draft.getId(), -1));
        assertThrows(DraftItemNotFoundException.class, () -> useCase().edit(author, draft.getId(), 5,
                new QuestionDraftItemPatch("x", null, null, null, null, null)));
        verify(drafts, never()).save(any());
    }

    @Test
    void onlyAReadyDraftOfItsOwnerCanBeChanged() throws Exception {
        AiDraft approved = draft(QuestionType.MULTIPLE_CHOICE, DraftStatus.APPROVED, CHOICE_ITEMS);
        AiDraft ready = draft(QuestionType.MULTIPLE_CHOICE, DraftStatus.READY, CHOICE_ITEMS);

        assertThrows(InvalidContentStateException.class, () -> useCase().remove(author, approved.getId(), 0));
        assertThrows(InvalidContentStateException.class, () -> useCase().edit(author, approved.getId(), 0,
                new QuestionDraftItemPatch("x", null, null, null, null, null)));
        assertThrows(AiDraftNotFoundException.class, () -> useCase().remove(UUID.randomUUID(), ready.getId(), 0));
        assertThrows(AiDraftNotFoundException.class, () -> useCase().remove(author, UUID.randomUUID(), 0));
        verify(drafts, never()).save(any());
    }

    @Test
    void aCourseDraftIsNotAQuestionDraft() throws Exception {
        AiDraft course = AiDraft.manual(DraftKind.COURSE, author, "{\"items\":[]}", Instant.now());
        when(drafts.findById(course.getId())).thenReturn(Optional.of(course));

        assertThrows(InvalidContentStateException.class, () -> useCase().remove(author, course.getId(), 0));
    }
}
