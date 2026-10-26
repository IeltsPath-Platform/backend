package com.ieltspath.library.api.controller;

import com.ieltspath.library.api.exception.GlobalExceptionHandler;
import com.ieltspath.library.application.result.VocabularyItemResult;
import com.ieltspath.library.application.result.VocabularySenseCardResult;
import com.ieltspath.library.application.usecase.AddVocabularySenseUseCase;
import com.ieltspath.library.application.usecase.CreateVocabularyItemUseCase;
import com.ieltspath.library.application.usecase.GetVocabularyDetailUseCase;
import com.ieltspath.library.application.usecase.GetVocabularySensesUseCase;
import com.ieltspath.library.application.usecase.SearchVocabularyUseCase;
import com.ieltspath.library.application.usecase.BrowseVocabularyUseCase;
import com.ieltspath.library.application.usecase.VocabularyCatalogUseCase;
import com.ieltspath.library.domain.vo.ContentStatus;
import com.ieltspath.library.domain.vo.PartOfSpeech;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VocabularySensesControllerTest {

    private final SearchVocabularyUseCase search = mock(SearchVocabularyUseCase.class);
    private final GetVocabularyDetailUseCase detail = mock(GetVocabularyDetailUseCase.class);
    private final CreateVocabularyItemUseCase create = mock(CreateVocabularyItemUseCase.class);
    private final AddVocabularySenseUseCase addSense = mock(AddVocabularySenseUseCase.class);
    private final GetVocabularySensesUseCase getSenses = mock(GetVocabularySensesUseCase.class);
    private final BrowseVocabularyUseCase browse = mock(BrowseVocabularyUseCase.class);
    private final VocabularyCatalogUseCase catalog = mock(VocabularyCatalogUseCase.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new VocabularyController(search, detail, getSenses, create, addSense, browse, catalog))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsSenseCardsForTheRequestedIds() throws Exception {
        UUID senseId = UUID.fromString("10000000-0000-4000-8000-000000000001");
        UUID itemId = UUID.fromString("20000000-0000-4000-8000-000000000001");
        when(getSenses.execute(List.of(senseId))).thenReturn(List.of(new VocabularySenseCardResult(
                senseId, itemId, "resilient", "/rɪˈzɪliənt/", "audio/resilient", PartOfSpeech.ADJECTIVE,
                "able to recover quickly", "kiên cường", "The town proved resilient after the storm.", null
        )));

        mvc.perform(get("/api/content/vocabulary/senses").param("ids", senseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].senseId").value(senseId.toString()))
                .andExpect(jsonPath("$[0].vocabularyItemId").value(itemId.toString()))
                .andExpect(jsonPath("$[0].lemma").value("resilient"))
                .andExpect(jsonPath("$[0].partOfSpeech").value("ADJECTIVE"))
                .andExpect(jsonPath("$[0].englishDefinition").value("able to recover quickly"))
                .andExpect(jsonPath("$[0].vietnameseMeaning").value("kiên cường"))
                .andExpect(jsonPath("$[0].exampleSentence").value("The town proved resilient after the storm."));
    }

    @Test
    void acceptsOneHundredIds() throws Exception {
        List<UUID> ids = IntStream.range(0, 100)
                .mapToObj(index -> UUID.nameUUIDFromBytes(("sense-" + index).getBytes()))
                .toList();
        when(getSenses.execute(ids)).thenReturn(List.of());

        mvc.perform(get("/api/content/vocabulary/senses")
                        .param("ids", ids.stream().map(UUID::toString).collect(Collectors.joining(","))))
                .andExpect(status().isOk());
        verify(getSenses).execute(ids);
    }

    @Test
    void rejectsEmptyMalformedAndMoreThanOneHundredIds() throws Exception {
        mvc.perform(get("/api/content/vocabulary/senses").param("ids", ""))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/content/vocabulary/senses").param("ids", "not-a-uuid"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/content/vocabulary/senses")
                        .param("ids", " 10000000-0000-4000-8000-000000000001"))
                .andExpect(status().isBadRequest());

        String tooManyIds = IntStream.range(0, 101)
                .mapToObj(index -> UUID.nameUUIDFromBytes(("sense-" + index).getBytes()).toString())
                .collect(Collectors.joining(","));
        mvc.perform(get("/api/content/vocabulary/senses").param("ids", tooManyIds))
                .andExpect(status().isBadRequest());
    }

    @Test
    void keepsTheExistingItemDetailRoute() throws Exception {
        UUID itemId = UUID.fromString("20000000-0000-4000-8000-000000000001");
        when(catalog.detail(itemId)).thenReturn(java.util.Map.of("id",itemId,"lemma","tree","senses",List.of()));

        mvc.perform(get("/api/content/vocabulary/{id}", itemId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lemma").value("tree"));
        verify(catalog).detail(itemId);
    }
}
