package com.ieltspath.content.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.api.dto.AccessLevel;
import com.ieltspath.content.api.dto.request.CreateQuestionRequest;
import com.ieltspath.content.api.exception.GlobalExceptionHandler;
import com.ieltspath.content.application.command.CreateQuestionCommand;
import com.ieltspath.content.application.result.QuestionDetailResult;
import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.application.result.QuestionVersionResult;
import com.ieltspath.content.application.usecase.*;
import com.ieltspath.content.domain.exception.QuestionNotFoundException;
import com.ieltspath.content.domain.exception.DifficultyRequiredException;
import com.ieltspath.content.domain.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuestionControllerTest {

    @Mock
    private ListQuestionsUseCase listQuestionsUseCase;

    @Mock
    private ListQuestionsWithIncludesUseCase listQuestionsWithIncludesUseCase;

    @Mock
    private GetQuestionDetailUseCase getQuestionDetailUseCase;

    @Mock
    private GetQuestionPassagesUseCase getQuestionPassagesUseCase;

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
                q1, QuestionType.MULTIPLE_CHOICE, Skill.READING, null,
                PublicationStatus.PUBLISHED, UUID.randomUUID(), Instant.now(), Instant.now()
        );

        when(listQuestionsUseCase.execute(Skill.READING, null, null, null, null, 0, 25))
                .thenReturn(new ListQuestionsUseCase.Page(1, 0, 25, List.of(qResult)));

        mockMvc.perform(get("/api/content/questions?skill=READING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(q1.toString()))
                .andExpect(jsonPath("$.content[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(25))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].ownedElsewhere").doesNotExist())
                .andExpect(jsonPath("$.content[0].stemExcerpt").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/content/admin/questions passes status, page and size to the use case")
    void shouldPassStatusAndPaging() throws Exception {
        when(listQuestionsUseCase.execute(null, QuestionPurpose.LEARNING, PublicationStatus.PUBLISHED, null, null, 2, 10))
                .thenReturn(new ListQuestionsUseCase.Page(23, 2, 10, List.of()));

        mockMvc.perform(get("/api/content/admin/questions?purpose=LEARNING&status=PUBLISHED&page=2&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.totalElements").value(23))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void codeQueryIsPassedToQuestionCatalogSearch() throws Exception {
        when(listQuestionsUseCase.execute(null, null, null, "Q-000042", null, 0, 25))
                .thenReturn(new ListQuestionsUseCase.Page(0, 0, 25, List.of()));

        mockMvc.perform(get("/api/content/admin/questions?code=Q-000042"))
                .andExpect(status().isOk());

        verify(listQuestionsUseCase).execute(null, null, null, "Q-000042", null, 0, 25);
    }

    @Test
    void unknownIncludeReturnsVietnameseBadRequest() throws Exception {
        mockMvc.perform(get("/api/content/admin/questions?include=ownership,unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Chỉ hỗ trợ bổ sung trạng thái sử dụng hoặc bản xem trước câu hỏi."));
    }

    @Test
    void includesQuestionOwnershipAndPreviewWhenRequested() throws Exception {
        UUID id = UUID.randomUUID();
        QuestionResult result = new QuestionResult(id, QuestionType.MULTIPLE_CHOICE, Skill.READING, null,
                PublicationStatus.PUBLISHED, UUID.randomUUID(), Instant.now(), Instant.now(),
                QuestionPurpose.LEARNING, null, "Q-000042").enriched(true, "Đoạn đầu…",
                List.of(new com.ieltspath.content.application.result.QuestionPreviewKnowledgePointResult("R1", "Đọc hiểu")), 5);
        when(listQuestionsWithIncludesUseCase.execute(Skill.READING, QuestionPurpose.LEARNING,
                PublicationStatus.PUBLISHED, null, 0, 25, java.util.Set.of("ownership", "preview"), false, null, null))
                .thenReturn(new ListQuestionsUseCase.Page(1, 0, 25, List.of(result)));

        mockMvc.perform(get("/api/content/admin/questions?skill=READING&purpose=LEARNING&status=PUBLISHED&include=ownership,preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ownedElsewhere").value(true))
                .andExpect(jsonPath("$.content[0].stemExcerpt").value("Đoạn đầu…"))
                .andExpect(jsonPath("$.content[0].knowledgePoints[0].code").value("R1"))
                .andExpect(jsonPath("$.content[0].knowledgePoints[0].name").value("Đọc hiểu"))
                .andExpect(jsonPath("$.content[0].knowledgePointTotal").value(5));
    }

    @Test
    void combinesOwnershipFreeAndKnowledgePointFilters() throws Exception {
        UUID pointId = UUID.randomUUID();
        UUID blockId = UUID.randomUUID();
        when(listQuestionsWithIncludesUseCase.execute(Skill.READING, QuestionPurpose.LEARNING,
                PublicationStatus.PUBLISHED, null, 0, 25, java.util.Set.of("ownership", "preview"), true,
                blockId, List.of(pointId))).thenReturn(new ListQuestionsUseCase.Page(0, 0, 25, List.of()));

        mockMvc.perform(get("/api/content/admin/questions?skill=READING&purpose=LEARNING&status=PUBLISHED"
                        + "&include=ownership,preview&onlyFree=true&exceptBlockId=" + blockId + "&knowledgePointIds=" + pointId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(listQuestionsWithIncludesUseCase).execute(Skill.READING, QuestionPurpose.LEARNING,
                PublicationStatus.PUBLISHED, null, 0, 25, java.util.Set.of("ownership", "preview"), true,
                blockId, List.of(pointId));
    }

    @Test
    @DisplayName("GET /api/content/admin/questions/versions/{id}/passages returns the passages of the version")
    void shouldReturnThePassagesOfAVersion() throws Exception {
        UUID version = UUID.randomUUID();
        UUID asset = UUID.randomUUID();
        when(getQuestionPassagesUseCase.execute(version)).thenReturn(List.of(new com.ieltspath.content.application.port.QuestionPassageReader.Passage(
                asset, "A. In autumn", com.ieltspath.content.application.port.QuestionPassageReader.Source.LESSON, "Reading lesson")));

        mockMvc.perform(get("/api/content/admin/questions/versions/" + version + "/passages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assetId").value(asset.toString()))
                .andExpect(jsonPath("$[0].text").value("A. In autumn"))
                .andExpect(jsonPath("$[0].source").value("LESSON"))
                .andExpect(jsonPath("$[0].sourceName").value("Reading lesson"));
    }

    @Test
    @DisplayName("a recording of the version comes with its transcript, link and length")
    void shouldReturnTheRecordingOfAVersion() throws Exception {
        UUID version = UUID.randomUUID();
        UUID asset = UUID.randomUUID();
        when(getQuestionPassagesUseCase.execute(version)).thenReturn(List.of(new com.ieltspath.content.application.port.QuestionPassageReader.Passage(
                asset, com.ieltspath.content.domain.vo.AssetType.AUDIO, "Transcript",
                com.ieltspath.content.application.port.QuestionPassageReader.Source.SECTION, "Listening section", "clip.mp3",
                com.ieltspath.content.domain.vo.AccessTier.PUBLIC, 95, "https://media.example/clip.mp3")));

        mockMvc.perform(get("/api/content/admin/questions/versions/" + version + "/passages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("AUDIO"))
                .andExpect(jsonPath("$[0].text").value("Transcript"))
                .andExpect(jsonPath("$[0].mediaUrl").value("https://media.example/clip.mp3"))
                .andExpect(jsonPath("$[0].durationSeconds").value(95));
    }

    @Test
    void invalidPagingReturns400() throws Exception {
        when(listQuestionsUseCase.execute(null, null, null, null, null, 0, 1000)).thenThrow(new IllegalArgumentException("size"));

        mockMvc.perform(get("/api/content/admin/questions?size=1000")).andExpect(status().isBadRequest());
    }

    @Test
    void importRefLookupReturnsPageOfAtMostOne() throws Exception {
        UUID id = UUID.randomUUID();
        QuestionResult found = new QuestionResult(id, QuestionType.MULTIPLE_CHOICE, Skill.READING, null,
                PublicationStatus.DRAFT, null, Instant.now(), Instant.now(), QuestionPurpose.LEARNING, "ref-1");
        when(listQuestionsUseCase.findByImportRef("ref-1"))
                .thenReturn(new ListQuestionsUseCase.Page(1, 0, 100, List.of(found)));

        mockMvc.perform(get("/api/content/admin/questions?importRef=ref-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].importRef").value("ref-1"))
                .andExpect(jsonPath("$.totalElements").value(1));
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
                "Think about the largest city.",
                QuestionDifficulty.EASY,
                PublicationStatus.PUBLISHED,
                Instant.now(), Instant.now(),
                List.of()
        );

        QuestionDetailResult detailResult = new QuestionDetailResult(
                questionId, QuestionType.MULTIPLE_CHOICE, Skill.READING, null,
                PublicationStatus.PUBLISHED, versionId, Instant.now(), Instant.now(),
                List.of(versionResult)
        );

        when(getQuestionDetailUseCase.execute(questionId)).thenReturn(detailResult);

        mockMvc.perform(get("/api/content/questions/" + questionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(questionId.toString()))
                .andExpect(jsonPath("$.versions[0].stem").value("What is the capital of France?"))
                .andExpect(jsonPath("$.versions[0].answerSpecJson").value("{\"correct\": \"A\"}"))
                .andExpect(jsonPath("$.versions[0].explanation").value("Paris is the capital of France."))
                .andExpect(jsonPath("$.versions[0].hint").value("Think about the largest city."));
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
                questionId, QuestionType.MULTIPLE_CHOICE, Skill.LISTENING, null,
                PublicationStatus.DRAFT, null, Instant.now(), Instant.now()
        );

        when(createQuestionUseCase.execute(any(CreateQuestionCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/content/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(questionId.toString()))
                .andExpect(jsonPath("$.skill").value("LISTENING"))
                .andExpect(jsonPath("$.purpose").value("LEARNING"));
        ArgumentCaptor<CreateQuestionCommand> command = ArgumentCaptor.forClass(CreateQuestionCommand.class);
        verify(createQuestionUseCase).execute(command.capture());
        assertThat(command.getValue().purpose()).isEqualTo(QuestionPurpose.LEARNING);
    }

    @Test
    @DisplayName("POST /api/content/questions/{id}/archive should archive question successfully")
    void shouldArchiveQuestion() throws Exception {
        UUID questionId = UUID.randomUUID();
        QuestionResult result = new QuestionResult(
                questionId, QuestionType.MULTIPLE_CHOICE, Skill.LISTENING, null,
                PublicationStatus.ARCHIVED, null, Instant.now(), Instant.now()
        );

        when(archiveQuestionUseCase.execute(questionId)).thenReturn(result);

        mockMvc.perform(post("/api/content/questions/" + questionId + "/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(questionId.toString()))
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void examQuestionsCanBeCreatedAndFilteredByPurpose() throws Exception {
        UUID questionId = UUID.randomUUID();
        QuestionResult exam = new QuestionResult(questionId, QuestionType.MULTIPLE_CHOICE, Skill.READING, null,
                PublicationStatus.DRAFT, null, Instant.now(), Instant.now(), QuestionPurpose.EXAM);
        when(createQuestionUseCase.execute(any())).thenReturn(exam);
        when(listQuestionsUseCase.execute(Skill.READING, QuestionPurpose.EXAM, null, null, null, 0, 25))
                .thenReturn(new ListQuestionsUseCase.Page(1, 0, 25, List.of(exam)));

        mockMvc.perform(post("/api/content/questions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionType\":\"MULTIPLE_CHOICE\",\"skill\":\"READING\",\"purpose\":\"EXAM\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.purpose").value("EXAM"));
        ArgumentCaptor<CreateQuestionCommand> command = ArgumentCaptor.forClass(CreateQuestionCommand.class);
        verify(createQuestionUseCase).execute(command.capture());
        assertThat(command.getValue().purpose()).isEqualTo(QuestionPurpose.EXAM);
        mockMvc.perform(get("/api/content/questions?skill=READING&purpose=EXAM"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].purpose").value("EXAM"));
    }

    @Test
    void invalidPurposeFilterReturns400() throws Exception {
        mockMvc.perform(get("/api/content/questions?purpose=INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingDifficultyReturns400WithErrorCode() throws Exception {
        UUID questionId = UUID.randomUUID();
        doThrow(new DifficultyRequiredException()).when(addQuestionVersionUseCase).execute(any());

        mockMvc.perform(post("/api/content/questions/" + questionId + "/versions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"versionNumber\":1,\"stem\":\"A question stem\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.code").value("DIFFICULTY_REQUIRED"));
    }
}
