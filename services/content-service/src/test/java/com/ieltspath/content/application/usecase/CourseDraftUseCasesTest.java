package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.service.CourseCatalogReaderStub;
import com.ieltspath.content.application.service.CourseDraftAnalysisService;
import com.ieltspath.content.application.service.CourseDraftCodec;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.exception.AiDraftNotFoundException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.exception.InvalidCourseDraftException;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseDraftUseCasesTest {

    private final ObjectMapper json = new ObjectMapper();
    private final AiDraftRepository store = mock(AiDraftRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T00:00:00Z"), ZoneOffset.UTC);
    private final CourseDraftUseCases useCases = new CourseDraftUseCases(store, new CourseDraftCodec(json),
            new CourseDraftAnalysisService(new CourseCatalogReaderStub()), clock);
    private final UUID author = UUID.randomUUID();

    {
        when(store.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    private AiDraft stored(UUID owner, DraftKind kind, DraftStatus status) {
        AiDraft draft = new AiDraft(UUID.randomUUID(), kind, status, owner, "{}", "{\"topics\":[]}", null, null, null,
                clock.instant(), null, null, null, null, null);
        when(store.findById(draft.getId())).thenReturn(Optional.of(draft));
        return draft;
    }

    @Test
    void aHandwrittenDraftIsReadyAtOnceWithoutAModel() throws Exception {
        AiDraft created = useCases.create(author, json.readTree("{\"course\":{\"code\":\"C1\",\"name\":\"N\","
                + "\"bandLevel\":6.5}}"));

        assertEquals(DraftStatus.READY, created.getStatus());
        assertEquals(DraftKind.COURSE, created.getKind());
        assertEquals(author, created.getRequestedBy());
        assertNull(created.getModel());
        assertTrue(created.getResultJson().contains("\"code\":\"C1\""));
    }

    @Test
    void noStructureMakesAnEmptyDraft() {
        AiDraft created = useCases.create(author, null);

        assertTrue(created.getResultJson().contains("\"topics\":[]"));
    }

    @Test
    void anInvalidStructureStoresNothing() throws Exception {
        assertThrows(InvalidCourseDraftException.class,
                () -> useCases.create(author, json.readTree("{\"course\":{\"code\":\"\"}}")));

        verify(store, never()).save(any());
    }

    @Test
    void savingReplacesTheStructureOfOwnReadyDraft() throws Exception {
        AiDraft draft = stored(author, DraftKind.COURSE, DraftStatus.READY);

        AiDraft saved = useCases.save(author, draft.getId(),
                json.readTree("{\"course\":{\"code\":\"C2\",\"name\":\"N\",\"bandLevel\":5.5}}"));

        assertTrue(saved.getResultJson().contains("\"code\":\"C2\""));
    }

    @Test
    void anotherAuthorOrAnotherKindIsNotFound() throws Exception {
        AiDraft others = stored(UUID.randomUUID(), DraftKind.COURSE, DraftStatus.READY);
        AiDraft questions = stored(author, DraftKind.QUESTIONS, DraftStatus.READY);

        assertThrows(AiDraftNotFoundException.class,
                () -> useCases.save(author, others.getId(), json.readTree("{}")));
        assertThrows(AiDraftNotFoundException.class, () -> useCases.analyze(author, questions.getId()));
        assertThrows(AiDraftNotFoundException.class, () -> useCases.analyze(author, UUID.randomUUID()));
    }

    @Test
    void onlyAReadyDraftCanBeSavedOrAnalysed() throws Exception {
        for (DraftStatus status : new DraftStatus[]{DraftStatus.QUEUED, DraftStatus.APPROVED, DraftStatus.REJECTED,
                DraftStatus.FAILED}) {
            AiDraft draft = stored(author, DraftKind.COURSE, status);
            assertThrows(InvalidContentStateException.class,
                    () -> useCases.save(author, draft.getId(), json.readTree("{}")), status.name());
            assertThrows(InvalidContentStateException.class, () -> useCases.analyze(author, draft.getId()),
                    status.name());
        }
    }

    @Test
    void analysingAnEmptyDraftIsValid() {
        AiDraft draft = stored(author, DraftKind.COURSE, DraftStatus.READY);

        assertTrue(useCases.analyze(author, draft.getId()).valid());
    }
}
