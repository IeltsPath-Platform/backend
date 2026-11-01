package com.ieltspath.library.api.controller;

import com.ieltspath.library.api.exception.GlobalExceptionHandler;
import com.ieltspath.library.application.result.WordLookupResult;
import com.ieltspath.library.application.usecase.LookupVocabularyWordUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VocabularyLookupControllerTest {

    private final LookupVocabularyWordUseCase lookup = mock(LookupVocabularyWordUseCase.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new VocabularyLookupController(lookup))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void returnsTheWordsWithTheirSensesAndPronunciation() throws Exception {
        when(lookup.execute(List.of("downloads", "download"))).thenReturn(List.of(
                new WordLookupResult("download", "download", "/ˈdaʊnləʊd/", "https://media/uk/download.mp3",
                        "https://media/us/download.mp3", List.of(
                        new WordLookupResult.Sense("VERB", "to copy a file", "tải xuống", "I download music."))),
                new WordLookupResult("live", "live", "/laɪv/", null, null, List.of())));

        mvc.perform(get("/api/content/vocabulary/lookup").param("forms", "downloads, download,"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lemma").value("download"))
                .andExpect(jsonPath("$[0].ipa").value("/ˈdaʊnləʊd/"))
                .andExpect(jsonPath("$[0].audioUk").value("https://media/uk/download.mp3"))
                .andExpect(jsonPath("$[0].audioUs").value("https://media/us/download.mp3"))
                .andExpect(jsonPath("$[1].audioUk").isEmpty())
                .andExpect(jsonPath("$[0].senses[0].vietnameseMeaning").value("tải xuống"))
                .andExpect(jsonPath("$[0].senses[0].partOfSpeech").value("VERB"))
                .andExpect(jsonPath("$[1].senses").isEmpty());

        verify(lookup).execute(List.of("downloads", "download"));
    }

    @Test
    void doesNotExposeInternalFields() throws Exception {
        when(lookup.execute(any())).thenReturn(List.of(new WordLookupResult("live", "live", "/laɪv/", null, null, List.of())));

        mvc.perform(get("/api/content/vocabulary/lookup").param("forms", "live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].status").doesNotExist())
                .andExpect(jsonPath("$[0].createdAt").doesNotExist());
    }

    @Test
    void refusesAnEmptyOrOversizedFormList() throws Exception {
        mvc.perform(get("/api/content/vocabulary/lookup").param("forms", " , ")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/content/vocabulary/lookup").param("forms", String.join(",", Collections.nCopies(11, "word"))))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/content/vocabulary/lookup").param("forms", "x".repeat(65))).andExpect(status().isBadRequest());

        verify(lookup, never()).execute(any());
    }
}
