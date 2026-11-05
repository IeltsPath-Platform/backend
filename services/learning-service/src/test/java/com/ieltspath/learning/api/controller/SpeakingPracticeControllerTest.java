package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.UploadRejectedException;
import com.ieltspath.learning.api.exception.GlobalExceptionHandler;
import com.ieltspath.learning.application.exception.InsufficientPointsException;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.SpeakingSubmissionResult;
import com.ieltspath.learning.application.service.SpeakingAvailability;
import com.ieltspath.learning.application.service.SpeakingPracticeSettings;
import com.ieltspath.learning.application.service.SpeakingSettingsFixture;
import com.ieltspath.learning.application.usecase.CompleteSpeakingSubmissionUseCase;
import com.ieltspath.learning.application.usecase.CreateSpeakingSubmissionUseCase;
import com.ieltspath.learning.application.usecase.GetSpeakingSubmissionUseCase;
import com.ieltspath.learning.application.usecase.SettleSpeakingSubmissionUseCase;
import com.ieltspath.learning.application.usecase.UploadSpeakingRecordingUseCase;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SpeakingPracticeControllerTest {
    private static final UUID USER = UUID.randomUUID();
    private static final UUID SUBMISSION = UUID.randomUUID();
    private static final UUID QUESTION = UUID.randomUUID();
    private static final String BASE = "/api/learning/speaking-practice";

    private final CreateSpeakingSubmissionUseCase create = mock(CreateSpeakingSubmissionUseCase.class);
    private final UploadSpeakingRecordingUseCase upload = mock(UploadSpeakingRecordingUseCase.class);
    private final CompleteSpeakingSubmissionUseCase complete = mock(CompleteSpeakingSubmissionUseCase.class);
    private final GetSpeakingSubmissionUseCase get = mock(GetSpeakingSubmissionUseCase.class);
    private final SettleSpeakingSubmissionUseCase settle = mock(SettleSpeakingSubmissionUseCase.class);
    private final SpeakingAvailability availability = mock(SpeakingAvailability.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
        when(currentUser.requireUserId()).thenReturn(USER);
        SpeakingPracticeSettings settings = SpeakingSettingsFixture.enabled();
        when(availability.isAvailable()).thenReturn(true);
        mvc = MockMvcBuilders.standaloneSetup(new SpeakingPracticeController(currentUser, create, upload, complete, get, settle,
                        settings, availability))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private static SpeakingSubmissionResult result(SpeakingSubmissionStatus status) {
        return new SpeakingSubmissionResult(SUBMISSION, UUID.randomUUID(), SpeakingScope.FULL, status, 5, null,
                Instant.parse("2026-11-04T05:00:00Z"), List.of(new SpeakingSubmissionResult.Recording(QUESTION,
                UUID.randomUUID(), 1, 1, false, null)));
    }

    private String body(boolean consent) {
        return """
                {"requestId":"%s","packageVersionId":"%s","consent":%s}""".formatted(UUID.randomUUID(), UUID.randomUUID(), consent);
    }

    @Test
    void termsTellTheCostAndTheLargestFile() throws Exception {
        mvc.perform(get(BASE + "/terms")).andExpect(status().isOk())
                .andExpect(jsonPath("$.pointCost").value(5))
                .andExpect(jsonPath("$.maxRecordingBytes").value(5L * 1024 * 1024))
                .andExpect(jsonPath("$.retentionDays").value(90));
    }

    @Test
    void termsSayGradingIsOffWhenItsServicesAreNotAvailable() throws Exception {
        when(availability.isAvailable()).thenReturn(false);

        mvc.perform(get(BASE + "/terms")).andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void createsASubmissionAndListsTheQuestionsToRecord() throws Exception {
        when(create.execute(eq(USER), any(), any(), any(), anyBoolean())).thenReturn(result(SpeakingSubmissionStatus.UPLOADING));

        mvc.perform(post(BASE + "/submissions").contentType(MediaType.APPLICATION_JSON).content(body(true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(SUBMISSION.toString()))
                .andExpect(jsonPath("$.status").value("UPLOADING"))
                .andExpect(jsonPath("$.recordings[0].questionVersionId").value(QUESTION.toString()))
                .andExpect(jsonPath("$.recordings[0].uploaded").value(false));
    }

    @Test
    void notEnoughPointsIs402AndCarriesTheCode() throws Exception {
        when(create.execute(any(), any(), any(), any(), anyBoolean()))
                .thenThrow(new LearningRequestException(402, "INSUFFICIENT_POINTS", "Grading costs 5 points"));

        mvc.perform(post(BASE + "/submissions").contentType(MediaType.APPLICATION_JSON).content(body(true)))
                .andExpect(status().isPaymentRequired()).andExpect(jsonPath("$.code").value("INSUFFICIENT_POINTS"));
    }

    @Test
    void aSubmissionAlreadyWaitingIs409WithItsId() throws Exception {
        when(create.execute(any(), any(), any(), any(), anyBoolean())).thenThrow(new LearningRequestException(409,
                "SPEAKING_SUBMISSION_IN_PROGRESS", "You already have a submission", SUBMISSION));

        mvc.perform(post(BASE + "/submissions").contentType(MediaType.APPLICATION_JSON).content(body(true)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.submissionId").value(SUBMISSION.toString()));
    }

    @Test
    void aBodyWithoutRequiredIdsIsRejected() throws Exception {
        mvc.perform(post(BASE + "/submissions").contentType(MediaType.APPLICATION_JSON).content("{\"consent\":true}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void uploadsOneRecordingAsMultipart() throws Exception {
        when(upload.execute(eq(USER), eq(SUBMISSION), eq(QUESTION), eq("audio/wav"), any(), eq(4200)))
                .thenReturn(result(SpeakingSubmissionStatus.UPLOADING));

        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, BASE + "/submissions/" + SUBMISSION + "/recordings/" + QUESTION)
                        .file(new MockMultipartFile("file", "answer.wav", "audio/wav", new byte[] {1, 2, 3}))
                        .param("durationMs", "4200"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UPLOADING"));
    }

    @Test
    void aRefusedRecordingIs422AndATooLargeOneIs413() throws Exception {
        when(upload.execute(any(), any(), any(), any(), any(), any()))
                .thenThrow(new UploadRejectedException(UploadRejectedException.Reason.TYPE_NOT_ALLOWED))
                .thenThrow(new UploadRejectedException(UploadRejectedException.Reason.TOO_LARGE));
        String url = BASE + "/submissions/" + SUBMISSION + "/recordings/" + QUESTION;
        MockMultipartFile file = new MockMultipartFile("file", "answer.txt", "text/plain", new byte[] {1});

        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, url).file(file))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("RECORDING_TYPE_NOT_ALLOWED"));
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, url).file(file))
                .andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.code").value("RECORDING_TOO_LARGE"));
    }

    @Test
    void anUnavailableStorageIs503() throws Exception {
        when(upload.execute(any(), any(), any(), any(), any(), any()))
                .thenThrow(StorageException.withoutObject("put", StorageException.Reason.UNAVAILABLE));

        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, BASE + "/submissions/" + SUBMISSION + "/recordings/" + QUESTION)
                        .file(new MockMultipartFile("file", "a.wav", "audio/wav", new byte[] {1})))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"));
    }

    @Test
    void completingAnswers202() throws Exception {
        when(complete.execute(USER, SUBMISSION)).thenReturn(result(SpeakingSubmissionStatus.QUEUED));

        mvc.perform(post(BASE + "/submissions/" + SUBMISSION + "/complete")).andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void missingRecordingsAre422() throws Exception {
        when(complete.execute(any(), any())).thenThrow(new LearningRequestException(422, "SPEAKING_RECORDINGS_MISSING",
                "2 question(s) have no recording yet", SUBMISSION));

        mvc.perform(post(BASE + "/submissions/" + SUBMISSION + "/complete"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("SPEAKING_RECORDINGS_MISSING"));
    }

    @Test
    void readsASubmissionOrAnswers404() throws Exception {
        when(get.execute(USER, SUBMISSION)).thenReturn(result(SpeakingSubmissionStatus.QUEUED));
        mvc.perform(get(BASE + "/submissions/" + SUBMISSION)).andExpect(status().isOk());

        UUID other = UUID.randomUUID();
        when(get.execute(USER, other)).thenThrow(new LearningRequestException(404, "SPEAKING_SUBMISSION_NOT_FOUND", "not found"));
        mvc.perform(get(BASE + "/submissions/" + other)).andExpect(status().isNotFound());
    }

    @Test
    void noResponseEverShowsAnObjectKey() throws Exception {
        when(get.execute(USER, SUBMISSION)).thenReturn(result(SpeakingSubmissionStatus.QUEUED));

        String json = mvc.perform(get(BASE + "/submissions/" + SUBMISSION)).andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertFalse(json.contains("objectKey") || json.contains("practice/"), json);
    }

    @Test
    void settleChargesTheLearnersOwnSubmissionAndReturnsIt() throws Exception {
        when(settle.execute(USER, SUBMISSION)).thenReturn(result(SpeakingSubmissionStatus.GRADED));

        mvc.perform(post(BASE + "/submissions/" + SUBMISSION + "/settle"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("GRADED"));
    }

    @Test
    void settleWithTooFewPointsIs402SoTheLearnerCanTopUp() throws Exception {
        when(settle.execute(USER, SUBMISSION)).thenThrow(new InsufficientPointsException());

        mvc.perform(post(BASE + "/submissions/" + SUBMISSION + "/settle"))
                .andExpect(status().isPaymentRequired()).andExpect(jsonPath("$.code").value("INSUFFICIENT_POINTS"));
    }
}