package com.ieltspath.learning.infrastructure.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.PlacementSpeakingJobStore;
import com.ieltspath.learning.application.port.SpeechAnalysisClient;
import com.ieltspath.learning.application.service.SpeakingGrader;
import com.ieltspath.learning.application.service.SpeakingPracticeSettings;
import com.ieltspath.learning.application.service.SpeakingResultAssembler;
import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.events.assessment.SpeakingGradingRequestedV1;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PlacementSpeakingGradingJobTest {
    @Test
    void gradingFailureAfterFinalAttemptPublishesFailedCompletion() throws Exception {
        var jobs=mock(PlacementSpeakingJobStore.class);
        var storage=mock(ObjectStorage.class);
        var speech=mock(SpeechAnalysisClient.class);
        var content=mock(LearningContentClient.class);
        var grader=mock(SpeakingGrader.class);
        var settings=mock(SpeakingPracticeSettings.class);
        when(settings.staleAfterSeconds()).thenReturn(300);
        when(speech.available()).thenReturn(true);
        when(grader.available()).thenReturn(true);
        when(content.getActiveRubric("SPEAKING")).thenThrow(new IllegalStateException("rubric unavailable"));
        UUID attemptId=UUID.randomUUID(); UUID userId=UUID.randomUUID(); UUID eventId=UUID.randomUUID();
        var request=new SpeakingGradingRequestedV1(eventId,SpeakingGradingRequestedV1.EVENT_TYPE,Instant.now(),
                SpeakingGradingRequestedV1.SOURCE,new SpeakingGradingRequestedV1.Data(attemptId,userId,List.of()));
        ObjectMapper json=new ObjectMapper().registerModule(new JavaTimeModule());
        when(jobs.claimDue(eq(5),eq(3),any(),any())).thenReturn(List.of(new PlacementSpeakingJobStore.Job(eventId,
                attemptId,userId,json.writeValueAsString(request),3,"correlation",null)));
        var job=new PlacementSpeakingGradingJob(jobs,storage,speech,content,grader,settings,json,3,5,30);

        job.poll();

        verify(jobs).complete(eq(eventId),eq("FAILED"),eq("GRADING_FAILED"),isNull(),argThat(event ->
                event.eventType().equals("SpeakingGradingCompleted.v1") && event.payload().contains("GRADING_FAILED")));
        verifyNoInteractions(storage);
        verify(grader,never()).prepare(any(),any());
        verify(grader,never()).grade(any(),any());
        verify(jobs,never()).retry(any(),any(),any());
    }

    /** The grader answers with the learner view under "display", as it does for practice. */
    @Test
    void gradedSectionPublishesTheMeanBandAndKeepsTheLearnerView() throws Exception {
        var jobs=mock(PlacementSpeakingJobStore.class);
        var storage=mock(ObjectStorage.class);
        var speech=mock(SpeechAnalysisClient.class);
        var content=mock(LearningContentClient.class);
        var grader=mock(SpeakingGrader.class);
        var settings=mock(SpeakingPracticeSettings.class);
        when(speech.available()).thenReturn(true);
        when(grader.available()).thenReturn(true);
        when(content.getActiveRubric("SPEAKING")).thenReturn(mock(LearningContentClient.Rubric.class));
        java.util.Map<String,Object> display=new java.util.LinkedHashMap<>();
        display.put("overallBand",null);
        display.put("summary","Clear but short.");
        display.put("criteria",java.util.stream.Stream.of("FC","LR","GRA","PR")
                .map(code -> java.util.Map.of("code",code,"band",new java.math.BigDecimal(code.equals("PR")?"7":"6"))).toList());
        when(grader.grade(any(),any())).thenReturn(new SpeakingResultAssembler.Assembled(
                java.util.Map.of("display",display,"signals",java.util.Map.of()),null));
        UUID eventId=UUID.randomUUID();
        var request=new SpeakingGradingRequestedV1(eventId,SpeakingGradingRequestedV1.EVENT_TYPE,Instant.now(),
                SpeakingGradingRequestedV1.SOURCE,new SpeakingGradingRequestedV1.Data(UUID.randomUUID(),UUID.randomUUID(),List.of()));
        ObjectMapper json=new ObjectMapper().registerModule(new JavaTimeModule());
        when(jobs.claimDue(eq(5),eq(3),any(),any())).thenReturn(List.of(new PlacementSpeakingJobStore.Job(eventId,
                request.data().attemptId(),request.data().userId(),json.writeValueAsString(request),1,"correlation",null)));

        new PlacementSpeakingGradingJob(jobs,storage,speech,content,grader,settings,json,3,5,30).poll();

        verify(jobs).complete(eq(eventId),eq("GRADED"),isNull(),
                argThat(result -> result.contains("\"overallBand\":6.5") && result.contains("Clear but short.")),
                argThat(event -> event.payload().contains("6.5") && event.payload().contains("\"PR\"")));
        verify(jobs,never()).retry(any(),any(),any());
    }

    @Test
    void unavailableSpeechOrLlmTerminatesAsFailedWithoutCallingGraderOrChargingPractice() throws Exception {
        var jobs=mock(PlacementSpeakingJobStore.class);
        var storage=mock(ObjectStorage.class);
        var speech=mock(SpeechAnalysisClient.class);
        var content=mock(LearningContentClient.class);
        var grader=mock(SpeakingGrader.class);
        var settings=mock(SpeakingPracticeSettings.class);
        when(settings.staleAfterSeconds()).thenReturn(300);
        UUID attemptId=UUID.randomUUID(); UUID userId=UUID.randomUUID(); UUID itemId=UUID.randomUUID(); UUID eventId=UUID.randomUUID();
        var request=new SpeakingGradingRequestedV1(eventId,SpeakingGradingRequestedV1.EVENT_TYPE,Instant.now(),
                SpeakingGradingRequestedV1.SOURCE,new SpeakingGradingRequestedV1.Data(attemptId,userId,
                List.of(new SpeakingGradingRequestedV1.Item(itemId,1,1,"Question","speaking/"+userId+"/recording.wav"))));
        ObjectMapper json=new ObjectMapper().registerModule(new JavaTimeModule());
        when(jobs.claimDue(eq(5),eq(3),any(),any())).thenReturn(List.of(new PlacementSpeakingJobStore.Job(eventId,
                attemptId,userId,json.writeValueAsString(request),3,"correlation",null)));
        when(speech.available()).thenReturn(false);
        var job=new PlacementSpeakingGradingJob(jobs,storage,speech,content,grader,settings,json,3,5,30);

        job.poll();

        verify(jobs).complete(eq(eventId),eq("FAILED"),eq("NOT_CONFIGURED"),isNull(),argThat(event ->
                event.eventType().equals("SpeakingGradingCompleted.v1") && event.payload().contains("NOT_CONFIGURED")));
        verifyNoInteractions(storage,content,grader);
        verify(jobs,never()).retry(any(),any(),any());
    }
}
