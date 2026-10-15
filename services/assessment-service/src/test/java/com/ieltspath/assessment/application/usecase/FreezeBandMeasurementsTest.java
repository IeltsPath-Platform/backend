package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.*;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.entity.*;
import com.ieltspath.assessment.domain.repository.*;
import com.ieltspath.assessment.domain.vo.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FreezeBandMeasurementsTest {
    private final BandStatisticsStore store=mock(BandStatisticsStore.class);
    private final AttemptSectionRepository sections=mock(AttemptSectionRepository.class);
    private final SkillScoreRepository scores=mock(SkillScoreRepository.class);
    private final GateEssayJobStore jobs=mock(GateEssayJobStore.class);
    private final FreezeBandMeasurements freezer=new FreezeBandMeasurements(store,sections,scores,jobs,new ObjectMapper());
    private final AssessmentAttempt attempt=AssessmentAttempt.start(UUID.randomUUID(),UUID.randomUUID(),AttemptType.PLACEMENT,AttemptMode.STANDARD,AttemptChannel.WEB,null);
    private final AssessmentResult result=new AssessmentResult(UUID.randomUUID(),attempt.getId(),1,"COMPLETED",5.5,Instant.now());
    private final List<AttemptSection> sectionList=new ArrayList<>();
    private final List<AttemptItem> items=new ArrayList<>();
    private final List<ItemResult> graded=new ArrayList<>();
    private List<AttemptItem> skill(Skill skill,int count) {
        UUID section=UUID.randomUUID();
        sectionList.add(new AttemptSection(section,attempt.getId(),UUID.randomUUID(),sectionList.size(),"{\"skill\":\""+skill+"\"}"));
        List<AttemptItem> added=new ArrayList<>();
        for(int i=0;i<count;i++) {
            var item=new AttemptItem(UUID.randomUUID(),section,UUID.randomUUID(),i,"{}",
                    "{\"answerSpec\":{\"task\":\"TASK_"+(i+1)+"\"},\"maxScore\":1}","{}");
            items.add(item);added.add(item);graded.add(new ItemResult(UUID.randomUUID(),result.id(),item.id(),1.0,1.0,true,null,"{}"));
        }
        return added;
    }
    @SuppressWarnings("unchecked") private Map<String,BandMeasurement> freeze() {
        when(sections.findByAttemptId(attempt.getId())).thenReturn(sectionList);
        freezer.freeze(attempt,result,items,graded);
        ArgumentCaptor<List<BandMeasurement>> captured=ArgumentCaptor.forClass(List.class);verify(store).freeze(eq(result.id()),captured.capture());
        Map<String,BandMeasurement> map=new HashMap<>();captured.getValue().forEach(m->map.put(m.skill(),m));return map;
    }
    @Test void fullObjectiveCoverageIsMeasuredButSpeakingConfigNeverCreatesOverall() {
        skill(Skill.LISTENING,40);skill(Skill.READING,40);skill(Skill.SPEAKING,1);
        var bands=freeze();assertTrue(bands.get("LISTENING").eligible());assertEquals(9.0,bands.get("READING").band());
        assertFalse(bands.get("SPEAKING").eligible());assertNull(bands.get("OVERALL").band());
    }
    @Test void shortObjectiveTestDoesNotMasqueradeAsFullBandMeasurement() {
        skill(Skill.LISTENING,10);var bands=freeze();assertEquals("INSUFFICIENT_COVERAGE",bands.get("LISTENING").eligibilityReason());
        assertFalse(bands.get("LISTENING").eligible());
    }
    @Test void fallbackEqualToRealBandStillExcludedByProvenance() {
        var writing=skill(Skill.WRITING,2);
        when(jobs.aiJobs(attempt.getId())).thenReturn(List.of(new GateEssayJobStore.JobState(writing.get(0).id(),"COMPLETED",new BigDecimal("5.5"),null,"MEASURED"),
                new GateEssayJobStore.JobState(writing.get(1).id(),"COMPLETED",new BigDecimal("5.5"),null,"FALLBACK")));
        assertEquals("FALLBACK",freeze().get("WRITING").measurementQuality());
    }
    @Test void measuredWritingUsesBothTasksAndDoubleTaskTwoWeight() {
        var writing=skill(Skill.WRITING,2);
        when(jobs.aiJobs(attempt.getId())).thenReturn(List.of(new GateEssayJobStore.JobState(writing.get(0).id(),"COMPLETED",new BigDecimal("6"),null,"MEASURED"),
                new GateEssayJobStore.JobState(writing.get(1).id(),"COMPLETED",new BigDecimal("7.5"),null,"MEASURED")));
        var band=freeze().get("WRITING");assertTrue(band.eligible());assertEquals(7.0,band.band());
    }
    @Test void unverifiedLearnerScoreCannotCreateSpeakingMeasurement() {
        skill(Skill.SPEAKING,1);
        when(scores.findByResultId(result.id())).thenReturn(List.of(new SkillScore(UUID.randomUUID(),result.id(),Skill.SPEAKING,null,9.0,GradingSource.HUMAN,null)));
        assertFalse(freeze().get("SPEAKING").eligible());
    }
    @Test void verifiedFourSkillCoverageComputesOverallFromSameResult() {
        skill(Skill.LISTENING,40);skill(Skill.READING,40);skill(Skill.WRITING,2);skill(Skill.SPEAKING,1);
        when(store.verifiedSkills(result.id())).thenReturn(Set.of(Skill.values()));
        when(scores.findByResultId(result.id())).thenReturn(Arrays.stream(Skill.values()).map(s->new SkillScore(UUID.randomUUID(),result.id(),s,null,6.5,GradingSource.HUMAN,null)).toList());
        assertEquals(6.5,freeze().get("OVERALL").band());
    }
}
