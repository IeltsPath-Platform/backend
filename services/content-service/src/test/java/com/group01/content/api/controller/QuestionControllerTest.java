package com.group01.content.api.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.content.api.dto.request.CreateQuestionRequest;
import com.group01.content.api.exception.GlobalExceptionHandler;
import com.group01.content.application.command.CreateQuestionCommand;
import com.group01.content.application.result.QuestionDetailResult;
import com.group01.content.application.result.QuestionResult;
import com.group01.content.application.result.QuestionVersionResult;
import com.group01.content.application.usecase.AddQuestionVersionUseCase;
import com.group01.content.application.usecase.ArchiveQuestionUseCase;
import com.group01.content.application.usecase.CreateQuestionUseCase;
import com.group01.content.application.usecase.GetQuestionDetailUseCase;
import com.group01.content.application.usecase.ListQuestionsUseCase;
import com.group01.content.domain.exception.QuestionNotFoundException;
import com.group01.content.domain.vo.AccessLevel;
import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.domain.vo.QuestionDifficulty;
import com.group01.content.domain.vo.QuestionOptionPayload;
import com.group01.content.domain.vo.QuestionType;
import com.group01.content.domain.vo.Skill;

@ExtendWith(MockitoExtension.class)
class QuestionControllerTest {

    @Mock
    private ListQuestionsUseCase listQuestionsUseCase;

    @Mock
    private GetQuestionDetailUseCase getQuestionDetailUseCase;

    @Mock
    private CreateQuestionUseCase createQuestionUseCase;

    @Mock
    private AddQuestionVersionUseCase addQuestionVersionUseCase;

    @Mock
    private ArchiveQuestionUseCase archiveQuestionUseCase;

    @InjectMocks
    private QuestionController questionController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(questionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/content/questions should return list of questions")
    void shouldReturnQuestions() throws Exception {
        UUID q1 = UUID.randomUUID();
        QuestionResult qResult = new QuestionResult(
                q1, QuestionType.MULTIPLE_CHOICE, Skill.READING, AccessLevel.FREE,
                PublicationStatus.PUBLISHED, UUID.randomUUID(), Instant.now(), Instant.now()
        );

        when(listQuestionsUseCase.execute(Skill.READING)).thenReturn(List.of(qResult));

        mockMvc.perform(get("/api/content/questions?skill=READING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(q1.toString()))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("GET /api/content/questions/{id} should return question detail with options and answerSpec")
    void shouldReturnQuestionDetail() throws Exception {
        UUID questionId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        QuestionVersionResult versionResult = new QuestionVersionResult(
                versionId, questionId, 1, "What is the capital of France?",
                List.of(new QuestionOptionPayload("A", "Paris", 1), new QuestionOptionPayload("B", "London", 2)),
                "{\"correct\": \"A\"}",
                "Paris is the capital of France.",
                QuestionDifficulty.EASY,
                PublicationStatus.PUBLISHED,
                Instant.now(), Instant.now(),
                List.of()
        );

        QuestionDetailResult detailResult = new QuestionDetailResult(
                questionId, QuestionType.MULTIPLE_CHOICE, Skill.READING, AccessLevel.FREE,
                PublicationStatus.PUBLISHED, versionId, Instant.now(), Instant.now(),
                List.of(versionResult)
        );

        when(getQuestionDetailUseCase.execute(questionId)).thenReturn(detailResult);

        mockMvc.perform(get("/api/content/questions/" + questionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(questionId.toString()))
                .andExpect(jsonPath("$.versions[0].stem").value("What is the capital of France?"))
                .andExpect(jsonPath("$.versions[0].answerSpecJson").value("{\"correct\": \"A\"}"))
                .andExpect(jsonPath("$.versions[0].explanation").value("Paris is the capital of France."));
    }

    @Test
    @DisplayName("GET /api/content/questions/{id} when not found should return 404")
    void shouldReturn404WhenNotFound() throws Exception {
        UUID questionId = UUID.randomUUID();
        when(getQuestionDetailUseCase.execute(questionId))
                .thenThrow(new QuestionNotFoundException(questionId));

        mockMvc.perform(get("/api/content/questions/" + questionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/content/questions should create question successfully")
    void shouldCreateQuestion() throws Exception {
        UUID questionId = UUID.randomUUID();
        CreateQuestionRequest request = new CreateQuestionRequest(
                QuestionType.MULTIPLE_CHOICE, Skill.LISTENING, AccessLevel.FREE
        );
        QuestionResult result = new QuestionResult(
                questionId, QuestionType.MULTIPLE_CHOICE, Skill.LISTENING, AccessLevel.FREE,
                PublicationStatus.DRAFT, null, Instant.now(), Instant.now()
        );

        when(createQuestionUseCase.execute(any(CreateQuestionCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/content/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(questionId.toString()))
                .andExpect(jsonPath("$.skill").value("LISTENING"));
    }

    @Test
    @DisplayName("POST /api/content/questions/{id}/archive should archive question successfully")
    void shouldArchiveQuestion() throws Exception {
        UUID questionId = UUID.randomUUID();
        QuestionResult result = new QuestionResult(
                questionId, QuestionType.MULTIPLE_CHOICE, Skill.LISTENING, AccessLevel.FREE,
                PublicationStatus.ARCHIVED, null, Instant.now(), Instant.now()
        );

        when(archiveQuestionUseCase.execute(questionId)).thenReturn(result);

        mockMvc.perform(post("/api/content/questions/" + questionId + "/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(questionId.toString()))
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }
}
