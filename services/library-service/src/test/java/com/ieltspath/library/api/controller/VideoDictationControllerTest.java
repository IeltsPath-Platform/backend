package com.ieltspath.library.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.library.api.exception.GlobalExceptionHandler;
import com.ieltspath.library.application.command.RecordVideoDictationAttemptCommand;
import com.ieltspath.library.application.result.DictationVideoDetailResult;
import com.ieltspath.library.application.result.DictationVideoListItemResult;
import com.ieltspath.library.application.result.VideoDictationAttemptResult;
import com.ieltspath.library.application.result.VideoDictationProgressResult;
import com.ieltspath.library.application.usecase.GetDictationVideoUseCase;
import com.ieltspath.library.application.usecase.GetVideoDictationProgressUseCase;
import com.ieltspath.library.application.usecase.ListDictationVideosUseCase;
import com.ieltspath.library.application.usecase.RecordVideoDictationAttemptUseCase;
import com.ieltspath.library.domain.exception.VideoNotFoundException;
import com.ieltspath.library.domain.vo.VideoLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real request mapping: it fails with a 400 when a parameter or path variable has no explicit name,
 * because the module is compiled without {@code -parameters}.
 */
class VideoDictationControllerTest {
    private final UUID userId = UUID.randomUUID();
    private final UUID videoId = UUID.randomUUID();
    private final UUID segmentId = UUID.randomUUID();

    private final ListDictationVideosUseCase list = mock(ListDictationVideosUseCase.class);
    private final GetDictationVideoUseCase detail = mock(GetDictationVideoUseCase.class);
    private final RecordVideoDictationAttemptUseCase record = mock(RecordVideoDictationAttemptUseCase.class);
    private final GetVideoDictationProgressUseCase progress = mock(GetVideoDictationProgressUseCase.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
        when(currentUser.requireUserId()).thenReturn(userId);
        mvc = MockMvcBuilders
                .standaloneSetup(new VideoDictationController(currentUser, list, detail, record, progress))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listsVideosAndPassesTheLevelFilter() throws Exception {
        when(list.execute(eq(userId), any())).thenReturn(List.of(new DictationVideoListItemResult(
                videoId, "yt", "Title", null, 60, VideoLevel.B1, 7, 3, java.time.Instant.parse("2026-10-20T03:26:45Z"))));

        mvc.perform(get("/api/learning-support/video-dictation/videos").param("level", "B1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Title"))
                .andExpect(jsonPath("$[0].level").value("B1"))
                .andExpect(jsonPath("$[0].passedSegments").value(3))
                .andExpect(jsonPath("$[0].lastPracticedAt").exists()); // the format is the application's Jackson setting
        verify(list).execute(userId, VideoLevel.B1);
    }

    @Test
    void listsAllLevelsWithoutTheParameter() throws Exception {
        when(list.execute(eq(userId), any())).thenReturn(List.of());

        mvc.perform(get("/api/learning-support/video-dictation/videos")).andExpect(status().isOk());
        verify(list).execute(userId, null);
    }

    @Test
    void rejectsAnUnknownLevel() throws Exception {
        mvc.perform(get("/api/learning-support/video-dictation/videos").param("level", "Z9"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void servesVideoDetailByPathVariable() throws Exception {
        when(detail.execute(videoId)).thenReturn(new DictationVideoDetailResult(videoId, "yt", "https://y", "Title",
                null, null, 60, VideoLevel.A2,
                List.of(new DictationVideoDetailResult.Segment(segmentId, 1, 0, 3000, "Hello world", "Xin chào",
                        java.util.Arrays.asList("/həˈloʊ/", null)))));

        mvc.perform(get("/api/learning-support/video-dictation/videos/{id}", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.segments[0].transcript").value("Hello world"))
                .andExpect(jsonPath("$.segments[0].endMs").value(3000))
                .andExpect(jsonPath("$.segments[0].ipaWords[0]").value("/həˈloʊ/"))
                .andExpect(jsonPath("$.segments[0].ipaWords[1]").doesNotExist());
    }

    @Test
    void unknownVideoIsNotFoundNotBadRequest() throws Exception {
        when(detail.execute(videoId)).thenThrow(new VideoNotFoundException(videoId));

        mvc.perform(get("/api/learning-support/video-dictation/videos/{id}", videoId)).andExpect(status().isNotFound());
    }

    @Test
    void servesProgressByPathVariable() throws Exception {
        when(progress.execute(userId, videoId)).thenReturn(new VideoDictationProgressResult(videoId, 7, 2, segmentId, 3,
                List.of(new VideoDictationProgressResult.SegmentProgress(segmentId, 3, 0, null, false))));

        mvc.perform(get("/api/learning-support/video-dictation/videos/{id}/progress", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passedSegments").value(2))
                .andExpect(jsonPath("$.nextSequenceNo").value(3));
    }

    @Test
    void recordsAnAttemptWithTheLearnerFromTheSession() throws Exception {
        when(record.execute(any())).thenReturn(new VideoDictationAttemptResult(UUID.randomUUID(), videoId, segmentId, 1,
                "She goes to school", true, new BigDecimal("100"), Instant.now()));

        mvc.perform(post("/api/learning-support/video-dictation/attempts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"segmentId\":\"" + segmentId + "\",\"typedText\":\"She goes to school\","
                                + "\"correct\":true,\"accuracyPercent\":100}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attemptNo").value(1))
                .andExpect(jsonPath("$.correct").value(true));

        ArgumentCaptor<RecordVideoDictationAttemptCommand> sent = ArgumentCaptor.forClass(RecordVideoDictationAttemptCommand.class);
        verify(record).execute(sent.capture());
        assertEquals(userId, sent.getValue().userId());
        assertEquals(segmentId, sent.getValue().segmentId());
    }

    @Test
    void rejectsAnAttemptWithBlankTextOrOutOfRangeAccuracy() throws Exception {
        String base = "\"segmentId\":\"" + segmentId + "\",\"correct\":false";
        mvc.perform(post("/api/learning-support/video-dictation/attempts").contentType(MediaType.APPLICATION_JSON)
                        .content("{" + base + ",\"typedText\":\" \",\"accuracyPercent\":10}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/learning-support/video-dictation/attempts").contentType(MediaType.APPLICATION_JSON)
                        .content("{" + base + ",\"typedText\":\"x\",\"accuracyPercent\":101}"))
                .andExpect(status().isBadRequest());
    }
}
