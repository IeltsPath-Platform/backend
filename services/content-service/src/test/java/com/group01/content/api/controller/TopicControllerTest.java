package com.group01.content.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.content.api.dto.request.CreateTopicRequest;
import com.group01.content.api.exception.GlobalExceptionHandler;
import com.group01.content.application.command.CreateTopicCommand;
import com.group01.content.application.result.TopicResult;
import com.group01.content.application.result.TopicTreeResult;
import com.group01.content.application.usecase.CreateTopicUseCase;
import com.group01.content.application.usecase.GetTopicTreeUseCase;
import com.group01.content.application.usecase.UpdateTopicUseCase;
import com.group01.content.domain.exception.DuplicateCodeException;
import com.group01.content.domain.vo.ContentStatus;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TopicControllerTest {

    @Mock
    private GetTopicTreeUseCase getTopicTreeUseCase;

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
                Instant.now(), Instant.now(), List.of()
        );
        when(getTopicTreeUseCase.execute()).thenReturn(List.of(treeNode));

        mockMvc.perform(get("/api/content/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(topicId.toString()))
                .andExpect(jsonPath("$[0].code").value("IELTS_READING"))
                .andExpect(jsonPath("$[0].name").value("Reading"));
    }

    @Test
    @DisplayName("POST /api/content/topics should create topic and return 201 Created")
    void shouldCreateTopicSuccessfully() throws Exception {
        UUID topicId = UUID.randomUUID();
        CreateTopicRequest request = new CreateTopicRequest(null, "IELTS_WRITING", "Writing", 2);
        TopicResult result = new TopicResult(
                topicId, null, "IELTS_WRITING", "Writing", 2, ContentStatus.ACTIVE,
                Instant.now(), Instant.now()
        );

        when(createTopicUseCase.execute(any(CreateTopicCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/content/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(topicId.toString()))
                .andExpect(jsonPath("$.code").value("IELTS_WRITING"))
                .andExpect(jsonPath("$.name").value("Writing"));
    }

    @Test
    @DisplayName("POST /api/content/topics with duplicate code should return 400 Bad Request")
    void shouldReturn400OnDuplicateCode() throws Exception {
        CreateTopicRequest request = new CreateTopicRequest(null, "DUPLICATE", "Duplicate Topic", 1);
        when(createTopicUseCase.execute(any(CreateTopicCommand.class)))
                .thenThrow(new DuplicateCodeException("Topic", "DUPLICATE"));

        mockMvc.perform(post("/api/content/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Topic with code 'DUPLICATE' already exists"));
    }
}
