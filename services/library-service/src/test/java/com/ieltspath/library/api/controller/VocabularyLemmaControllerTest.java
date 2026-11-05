package com.ieltspath.library.api.controller;

import com.ieltspath.library.api.exception.GlobalExceptionHandler;
import com.ieltspath.library.application.command.ResolveVocabularyCommand;
import com.ieltspath.library.application.result.ResolvedVocabularyResult;
import com.ieltspath.library.application.usecase.FindVocabularyByLemmasUseCase;
import com.ieltspath.library.application.usecase.ResolveVocabularyUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VocabularyLemmaControllerTest {

    private final FindVocabularyByLemmasUseCase find = mock(FindVocabularyByLemmasUseCase.class);
    private final ResolveVocabularyUseCase resolve = mock(ResolveVocabularyUseCase.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new VocabularyLemmaController(find, resolve))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void looksUpWordsByTheirExactLemmas() throws Exception {
        when(find.execute(List.of("trend", "plummet"))).thenReturn(List.of());

        mvc.perform(get("/api/content/vocabulary/by-lemmas").param("lemmas", "trend, plummet,"))
                .andExpect(status().isOk());

        verify(find).execute(List.of("trend", "plummet"));
    }

    @Test
    void refusesAnEmptyOrOversizedLemmaList() throws Exception {
        mvc.perform(get("/api/content/vocabulary/by-lemmas").param("lemmas", " , "))
                .andExpect(status().isBadRequest());
        String many = String.join(",", java.util.Collections.nCopies(101, "word"));
        mvc.perform(get("/api/content/vocabulary/by-lemmas").param("lemmas", many))
                .andExpect(status().isBadRequest());

        verify(find, never()).execute(any());
    }

    @Test
    void resolvesReviewedWordsAndReturnsTheirSenses() throws Exception {
        UUID sense = UUID.fromString("10000000-0000-4000-8000-000000000001");
        when(resolve.execute(any(ResolveVocabularyCommand.class)))
                .thenReturn(List.of(new ResolvedVocabularyResult("trend", sense, true)));

        mvc.perform(post("/api/content/admin/vocabulary/bulk").contentType(MediaType.APPLICATION_JSON).content("""
                        {"entries":[{"lemma":"Trend","ipa":"/trend/","partOfSpeech":"NOUN","englishDefinition":"a general direction",
                        "vietnameseMeaning":"xu hướng","exampleSentence":"There is an upward trend."}]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lemma").value("trend"))
                .andExpect(jsonPath("$[0].senseId").value(sense.toString()))
                .andExpect(jsonPath("$[0].created").value(true));

        ArgumentCaptor<ResolveVocabularyCommand> captured = ArgumentCaptor.forClass(ResolveVocabularyCommand.class);
        verify(resolve).execute(captured.capture());
        assertThat(captured.getValue().entries()).singleElement()
                .satisfies(entry -> assertThat(entry.vietnameseMeaning()).isEqualTo("xu hướng"));
    }

    @Test
    void refusesAnEntryWithoutMeaningOrExample() throws Exception {
        mvc.perform(post("/api/content/admin/vocabulary/bulk").contentType(MediaType.APPLICATION_JSON).content("""
                        {"entries":[{"lemma":"trend","partOfSpeech":"NOUN","vietnameseMeaning":" ","exampleSentence":""}]}
                        """))
                .andExpect(status().isBadRequest());

        verify(resolve, never()).execute(any());
    }
}
