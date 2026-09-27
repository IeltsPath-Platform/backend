package com.group01.content.api.controller;

import com.group01.content.api.exception.GlobalExceptionHandler;
import com.group01.content.application.result.ReadingPassageResult;
import com.group01.content.application.usecase.GetReadingPassageUseCase;
import com.group01.content.domain.exception.ReadingPassageNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReadingControllerTest {

    @Mock
    private GetReadingPassageUseCase getReadingPassageUseCase;

    @InjectMocks
    private ReadingController readingController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(readingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsLabelledParagraphs() throws Exception {
        UUID sectionId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        when(getReadingPassageUseCase.execute(sectionId)).thenReturn(new ReadingPassageResult(
                sectionId, "Rooftops", "Read the passage.", packageId, "Practice set",
                List.of(new ReadingPassageResult.Paragraph("A", "First."),
                        new ReadingPassageResult.Paragraph("B", "Second."))));

        mockMvc.perform(get("/api/content/reading/sections/{id}", sectionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionId").value(sectionId.toString()))
                .andExpect(jsonPath("$.sectionTitle").value("Rooftops"))
                .andExpect(jsonPath("$.instructions").value("Read the passage."))
                .andExpect(jsonPath("$.packageId").value(packageId.toString()))
                .andExpect(jsonPath("$.packageTitle").value("Practice set"))
                .andExpect(jsonPath("$.paragraphs[1].label").value("B"))
                .andExpect(jsonPath("$.paragraphs[1].text").value("Second."));
    }

    @Test
    void unreadableSectionsAreNotFound() throws Exception {
        UUID sectionId = UUID.randomUUID();
        when(getReadingPassageUseCase.execute(sectionId)).thenThrow(new ReadingPassageNotFoundException(sectionId));

        mockMvc.perform(get("/api/content/reading/sections/{id}", sectionId))
                .andExpect(status().isNotFound());
    }
}
