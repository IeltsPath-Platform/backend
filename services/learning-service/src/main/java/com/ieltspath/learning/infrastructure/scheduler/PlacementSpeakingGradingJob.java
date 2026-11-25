package com.ieltspath.learning.infrastructure.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.events.assessment.SpeakingGradingCompletedV1;
import com.ieltspath.events.assessment.SpeakingGradingRequestedV1;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.PlacementSpeakingJobStore;
import com.ieltspath.learning.application.port.SpeechAnalysisClient;
import com.ieltspath.learning.application.service.SpeakingGrader;
import com.ieltspath.learning.application.service.SpeakingGradingInput;
import com.ieltspath.learning.application.service.SpeakingPracticeSettings;
import com.ieltspath.learning.application.service.SpeakingPromptAssembler;
import com.ieltspath.learning.application.service.SpeakingResultAssembler;
import com.ieltspath.learning.domain.service.PronunciationBandMapper;
import com.ieltspath.learning.domain.service.SpeakingAnalysisSummary;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import com.ieltspath.learning.domain.vo.SpeechAnalysis;
import com.ieltspath.learning.domain.vo.TranscriptQuality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Claims durable placement Speaking jobs, calling storage, Azure and the LLM only after the claim transaction ends. */
@Component
@EnableScheduling
public class PlacementSpeakingGradingJob {
    private static final Logger log = LoggerFactory.getLogger(PlacementSpeakingGradingJob.class);
    private final PlacementSpeakingJobStore jobs;
    private final ObjectStorage storage;
    private final SpeechAnalysisClient speech;
    private final LearningContentClient content;
    private final SpeakingGrader grader;
    private final SpeakingPracticeSettings settings;
    private final ObjectMapper json;
    private final int maxAttempts;
    private final int batchSize;
    private final int retrySeconds;

    public PlacementSpeakingGradingJob(PlacementSpeakingJobStore jobs, ObjectStorage storage,
                                      SpeechAnalysisClient speech, LearningContentClient content,
                                      SpeakingGrader grader, SpeakingPracticeSettings settings, ObjectMapper json,
                                      @Value("${learning.speaking-grading.max-attempts:3}") int maxAttempts,
                                      @Value("${learning.speaking-grading.batch-size:5}") int batchSize,
                                      @Value("${learning.speaking-grading.retry-delay-seconds:30}") int retrySeconds) {
        this.jobs=jobs; this.storage=storage; this.speech=speech; this.content=content; this.grader=grader;
        this.settings=settings; this.json=json; this.maxAttempts=maxAttempts; this.batchSize=batchSize;
        this.retrySeconds=retrySeconds;
    }

    @Scheduled(fixedDelayString="${learning.speaking-grading.poll-ms:5000}")
    public void poll() {
        Instant now=Instant.now();
        List<PlacementSpeakingJobStore.Job> claimed=jobs.claimDue(batchSize,maxAttempts,now,
                now.minusSeconds(settings.staleAfterSeconds()));
        for(var job:claimed) process(job);
    }

    private void process(PlacementSpeakingJobStore.Job job) {
        try {
            SpeakingGradingRequestedV1 request=json.readValue(job.payload(),SpeakingGradingRequestedV1.class);
            if(!speech.available() || !grader.available()) throw new IllegalStateException("NOT_CONFIGURED");
            var rubric=content.getActiveRubric("SPEAKING");
            List<SpeechAnalysis> analyses=new ArrayList<>();
            List<SpeakingGradingInput.Answer> answers=new ArrayList<>();
            for(var item:request.data().items()) {
                byte[] wav=storage.download(settings.bucket(),item.objectKey());
                SpeechAnalysis analysis=speech.analyze(wav,null);
                analyses.add(analysis);
                answers.add(new SpeakingGradingInput.Answer(item.partNo(),item.questionNo(),item.question(),analysis));
            }
            var input=new SpeakingGradingInput(SpeakingScope.PART,answers,TranscriptQuality.of(analyses),
                    SpeakingAnalysisSummary.of(analyses,PronunciationBandMapper.INTERIM,10),
                    com.ieltspath.learning.application.service.SpeakingHistorySummarizer.LearnerContext.empty(),
                    List.of(),settings.maxInputChars());
            var prepared=grader.prepare(rubric,input);
            var result=grader.grade(prepared,input);
            publish(job,"GRADED",null,result,result.result(),rubric.code(),rubric.version());
        } catch(Exception failure) {
            String code=failureCode(failure);
            if(job.attempts()<maxAttempts) {
                jobs.retry(job.eventId(),code,Instant.now().plusSeconds(retrySeconds));
                log.warn("Placement Speaking job deferred: eventId={}, attempt={}, code={}",job.eventId(),job.attempts(),code);
            } else {
                publish(job,"FAILED",code,null,Map.of(),null,null);
            }
        }
    }

    private void publish(PlacementSpeakingJobStore.Job job,String status,String failureCode,
                         SpeakingResultAssembler.Assembled assembled,Map<String,Object> result,String rubricCode,Integer rubricVersion) {
        List<SpeakingGradingCompletedV1.Criterion> criteria=new ArrayList<>();
        BigDecimal band=null; String summary=null;
        if(assembled!=null) {
            JsonNode root=json.valueToTree(result);
            JsonNode rows=root.path("criteria");
            BigDecimal sum=BigDecimal.ZERO;
            for(JsonNode row:rows) {
                BigDecimal criterionBand=row.path("band").isNumber()?row.path("band").decimalValue():null;
                String code=row.path("code").asText("");
                String comment=row.path("comment").isTextual()?row.path("comment").asText():null;
                if(criterionBand!=null)sum=sum.add(criterionBand);
                criteria.add(new SpeakingGradingCompletedV1.Criterion(code,criterionBand,comment));
            }
            if(criteria.size()!=4 || criteria.stream().anyMatch(row -> row.band()==null || row.code().isBlank())) {
                if(job.attempts()<maxAttempts) {
                    jobs.retry(job.eventId(),"INVALID_RESULT",Instant.now().plusSeconds(retrySeconds));
                } else {
                    publish(job,"FAILED","INVALID_RESULT",null,Map.of(),null,null);
                }
                return;
            }
            band=sum.divide(BigDecimal.valueOf(4),4,RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(2))
                    .setScale(0,RoundingMode.HALF_UP).divide(BigDecimal.valueOf(2),1,RoundingMode.HALF_UP);
            summary=root.path("summary").isTextual()?root.path("summary").asText():null;
        }
        Instant now=Instant.now();
        var event=new SpeakingGradingCompletedV1(UUID.randomUUID(),SpeakingGradingCompletedV1.EVENT_TYPE,now,
                SpeakingGradingCompletedV1.SOURCE,new SpeakingGradingCompletedV1.Data(job.attemptId(),job.userId(),
                status,band,criteria,summary,rubricCode,rubricVersion,failureCode));
        try {
            String payload=json.writeValueAsString(event);
            jobs.complete(job.eventId(),status,failureCode,new PlacementSpeakingJobStore.OutboxEvent(event.eventId(),
                    event.eventType(),payload,now,job.correlationId(),job.traceContext()));
        } catch(Exception serializationFailure) {
            jobs.retry(job.eventId(),"EVENT_SERIALIZATION",Instant.now().plusSeconds(retrySeconds));
            log.error("Placement Speaking result event could not be serialized: eventId={}",job.eventId());
        }
    }

    private static String failureCode(Exception failure) {
        if("NOT_CONFIGURED".equals(failure.getMessage()))return "NOT_CONFIGURED";
        String name=failure.getClass().getSimpleName();
        if(name.contains("Speech"))return "SPEECH_FAILED";
        if(name.contains("Storage"))return "RECORDING_UNAVAILABLE";
        if(name.contains("Rubric"))return "RUBRIC_UNAVAILABLE";
        if(name.contains("Transcript"))return "TRANSCRIPT_TOO_LONG";
        if(name.contains("Llm")||name.contains("LLM"))return "LLM_FAILED";
        return "GRADING_FAILED";
    }
}
