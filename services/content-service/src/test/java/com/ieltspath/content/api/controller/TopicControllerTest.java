package com.ieltspath.content.api.controller;

import com.ieltspath.content.domain.vo.BandRange;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.api.dto.request.CreateTopicRequest;
import com.ieltspath.content.api.exception.GlobalExceptionHandler;
import com.ieltspath.content.application.command.CreateTopicCommand;
import com.ieltspath.content.application.result.TopicResult;
import com.ieltspath.content.application.result.TopicTreeResult;
import com.ieltspath.content.application.usecase.CreateTopicUseCase;
import com.ieltspath.content.application.usecase.GetTopicTreeUseCase;
import com.ieltspath.content.application.usecase.GetTopicUseCase;
import com.ieltspath.content.application.usecase.UpdateTopicUseCase;
import com.ieltspath.content.domain.exception.DuplicateCodeException;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.application.command.UpdateTopicCommand;
import com.ieltspath.content.domain.exception.TopicSkillLockedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TopicControllerTest {
    @Test
    void courseMembershipPassesThroughCreateUpdateAndReadResponses() throws Exception {
        UUID id = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        TopicResult result = new TopicResult(id, null, "TOPIC", "Topic", 1, ContentStatus.ACTIVE,
                Instant.now(), Instant.now(), BandRange.UNBOUNDED, null, courseId);
        when(createTopicUseCase.execute(any())).thenReturn(result);
        when(updateTopicUseCase.execute(any())).thenReturn(result);
        when(getTopicUseCase.execute(id)).thenReturn(result);
        when(getTopicTreeUseCase.execute()).thenReturn(List.of(new TopicTreeResult(id, null, "TOPIC", "Topic", 1,
                ContentStatus.ACTIVE, Instant.now(), Instant.now(), BandRange.UNBOUNDED, null, List.of(), courseId)));
        String body = "{\"code\":\"TOPIC\",\"name\":\"Topic\",\"sortOrder\":1,\"courseId\":\"" + courseId + "\"}";
        mockMvc.perform(post("/api/content/admin/topics").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.courseId").value(courseId.toString()));
        mockMvc.perform(put("/api/content/admin/topics/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.courseId").value(courseId.toString()));
        mockMvc.perform(get("/api/content/topics/{id}", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.courseId").value(courseId.toString()));
        mockMvc.perform(get("/api/content/topics"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].courseId").value(courseId.toString()));
        ArgumentCaptor<CreateTopicCommand> create = ArgumentCaptor.forClass(CreateTopicCommand.class);
        verify(createTopicUseCase).execute(create.capture());
        assertThat(create.getValue().courseId()).isEqualTo(courseId);
        ArgumentCaptor<UpdateTopicCommand> update = ArgumentCaptor.forClass(UpdateTopicCommand.class);
        verify(updateTopicUseCase).execute(update.capture());
        assertThat(update.getValue().courseId()).isEqualTo(courseId);
    }


    @Mock
    private GetTopicTreeUseCase getTopicTreeUseCase;
    @Mock
    private GetTopicUseCase getTopicUseCase;

    @Mock
    private CreateTopicUseCase createTopicUseCase;

    @Mock
    private UpdateTopicUseCase updateTopicUseCase;

    @InjectMocks
    private TopicController topicController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(topicController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/content/topics should return topic tree")
    void shouldReturnTopicTree() throws Exception {
        UUID topicId = UUID.randomUUID();
        TopicTreeResult treeNode = new TopicTreeResult(
                topicId, null, "IELTS_READING", "Reading", 1, ContentStatus.ACTIVE,
                Instant.now(), Instant.now(), BandRange.of(new BigDecimal("5.0"), new BigDecimal("6.5")), Skill.READING, List.of()
        );
        when(getTopicTreeUseCase.execute()).thenReturn(List.of(treeNode));

        mockMvc.perform(get("/api/content/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(topicId.toString()))
                .andExpect(jsonPath("$[0].code").value("IELTS_READING"))
                .andExpect(jsonPath("$[0].name").value("Reading"))
                .andExpect(jsonPath("$[0].bandMin").value(5.0))
                .andExpect(jsonPath("$[0].bandMax").value(6.5))
                .andExpect(jsonPath("$[0].skill").value("READING"));
    }

    @Test
    void shouldReturnTopicByIdForLibrary() throws Exception {
        UUID id = UUID.randomUUID();
        when(getTopicUseCase.execute(id)).thenReturn(new TopicResult(
                id, null, "READING", "Reading", 1, ContentStatus.ACTIVE,
                Instant.now(), Instant.now(), BandRange.UNBOUNDED, null));

        mockMvc.perform(get("/api/content/topics/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("POST /api/content/topics should create topic and return 201 Created")
    void shouldCreateTopicSuccessfully() throws Exception {
        UUID topicId = UUID.randomUUID();
        CreateTopicRequest request = new CreateTopicRequest(null, "IELTS_WRITING", "Writing", 2,
                new BigDecimal("6.0"), null, Skill.WRITING);
        TopicResult result = new TopicResult(
                topicId, null, "IELTS_WRITING", "Writing", 2, ContentStatus.ACTIVE,
                Instant.now(), Instant.now(), BandRange.of(new BigDecimal("6.0"), null), Skill.WRITING
        );

        when(createTopicUseCase.execute(any(CreateTopicCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/content/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(topicId.toString()))
                .andExpect(jsonPath("$.code").value("IELTS_WRITING"))
                .andExpect(jsonPath("$.name").value("Writing"))
                .andExpect(jsonPath("$.bandMin").value(6.0))
                .andExpect(jsonPath("$.bandMax").doesNotExist())
                .andExpect(jsonPath("$.skill").value("WRITING"));

        ArgumentCaptor<CreateTopicCommand> command = ArgumentCaptor.forClass(CreateTopicCommand.class);
        verify(createTopicUseCase).execute(command.capture());
        assertThat(command.getValue().band()).isEqualTo(BandRange.of(new BigDecimal("6.0"), null));
        assertThat(command.getValue().skill()).isEqualTo(Skill.WRITING);
    }

    @Test
    @DisplayName("POST /api/content/topics with an invalid band range should return 400 Bad Request")
    void shouldRejectInvalidBandRange() throws Exception {
        for (String band : new String[]{"\"bandMin\":7.0,\"bandMax\":5.0", "\"bandMin\":4.3", "\"bandMax\":9.5"}) {
            mockMvc.perform(post("/api/content/topics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"BAND\",\"name\":\"Band\",\"sortOrder\":0," + band + "}"))
                    .andExpect(status().isBadRequest());
        }

        verify(createTopicUseCase, never()).execute(any());
    }

    @Test
    void passesTheRequestedSkillToTheUseCaseAndMapsItsRejectionTo400() throws Exception {
        // The aggregate rejects ALL (see TopicTest); here the request reaches it unchanged and the refusal becomes 400.
        when(createTopicUseCase.execute(any(CreateTopicCommand.class)))
                .thenThrow(new IllegalArgumentException("A topic teaches one skill; ALL is not allowed"));
        mockMvc.perform(post("/api/content/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"MIX\",\"name\":\"Mix\",\"sortOrder\":0,\"skill\":\"ALL\"}"))
                .andExpect(status().isBadRequest());

        ArgumentCaptor<CreateTopicCommand> command = ArgumentCaptor.forClass(CreateTopicCommand.class);
        verify(createTopicUseCase).execute(command.capture());
        assertThat(command.getValue().skill()).isEqualTo(Skill.ALL);
    }

    @Test
    void changingTheSkillOfATopicWithPublishedLessonsReturns409() throws Exception {
        UUID id = UUID.randomUUID();
        when(updateTopicUseCase.execute(any(UpdateTopicCommand.class)))
                .thenThrow(new TopicSkillLockedException("DEMO_READING"));

        mockMvc.perform(put("/api/content/topics/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Reading\",\"sortOrder\":900,\"skill\":\"LISTENING\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.code").value("TOPIC_SKILL_LOCKED"));

        ArgumentCaptor<UpdateTopicCommand> command = ArgumentCaptor.forClass(UpdateTopicCommand.class);
        verify(updateTopicUseCase).execute(command.capture());
        assertThat(command.getValue().skill()).isEqualTo(Skill.LISTENING);
    }

    @Test
    @DisplayName("POST /api/content/topics with duplicate code should return 400 Bad Request")
    void shouldReturn400OnDuplicateCode() throws Exception {
        CreateTopicRequest request = new CreateTopicRequest(null, "DUPLICATE", "Duplicate Topic", 1, null, null, null);
        when(createTopicUseCase.execute(any(CreateTopicCommand.class)))
                .thenThrow(new DuplicateCodeException("Topic", "DUPLICATE"));

        mockMvc.perform(post("/api/content/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Topic with code 'DUPLICATE' already exists"));
    }
}
