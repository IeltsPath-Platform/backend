package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.BandStatisticsStore;
import com.ieltspath.assessment.application.port.GateEssayJobStore;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.entity.*;
import com.ieltspath.assessment.domain.repository.AttemptSectionRepository;
import com.ieltspath.assessment.domain.repository.SkillScoreRepository;
import com.ieltspath.assessment.domain.service.PlacementBandCalculator;
import com.ieltspath.assessment.domain.vo.*;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FreezeBandMeasurements {
    private final BandStatisticsStore store;
    private final AttemptSectionRepository sections;
    private final SkillScoreRepository scores;
    private final GateEssayJobStore jobs;
    private final ObjectMapper json;
    public FreezeBandMeasurements(BandStatisticsStore store, AttemptSectionRepository sections,
                                   SkillScoreRepository scores, GateEssayJobStore jobs, ObjectMapper json) {
        this.store=store;this.sections=sections;this.scores=scores;this.jobs=jobs;this.json=json;
    }
    public void freeze(AssessmentAttempt attempt, AssessmentResult result, List<AttemptItem> items, List<ItemResult> graded) {
        Map<UUID,Skill> skillBySection=new HashMap<>();
        for(var section:sections.findByAttemptId(attempt.getId())) {
            try { skillBySection.put(section.id(),Skill.valueOf(json.readTree(section.snapshot()).path("skill").asText())); }
            catch(Exception ignored) { /* Legacy sections without a skill cannot establish coverage. */ }
        }
        Map<Skill,List<AttemptItem>> bySkill=new EnumMap<>(Skill.class);
        for(var item:items) {
            Skill skill=skillBySection.get(item.attemptSectionId());
            if(skill!=null) bySkill.computeIfAbsent(skill,k->new ArrayList<>()).add(item);
        }
        var gradedByItem=graded.stream().collect(Collectors.toMap(ItemResult::attemptItemId,Function.identity()));
        var jobByItem=jobs.aiJobs(attempt.getId()).stream().collect(Collectors.toMap(GateEssayJobStore.JobState::attemptItemId,Function.identity(),(a,b)->b));
        var scoreBySkill=scores.findByResultId(result.id()).stream().collect(Collectors.toMap(SkillScore::skill,Function.identity()));
        Set<Skill> verified=store.verifiedSkills(result.id());
        List<BandMeasurement> snapshot=new ArrayList<>();
        boolean supported=Set.of(AttemptType.PLACEMENT,AttemptType.MOCK,AttemptType.OFFICIAL_PRACTICE).contains(attempt.getAttemptType());
        for(Skill skill:Skill.values()) {
            var skillItems=bySkill.getOrDefault(skill,List.of());
            String version=attempt.getAttemptType()==AttemptType.PLACEMENT?"placement-v1":"rubric-v1";
            BandMeasurement value=new BandMeasurement(skill.name(),null,"NONE","NOT_ASSESSED",version,
                    supported?"NOT_ASSESSED":"UNSUPPORTED_SERIES");
            if(supported && !skillItems.isEmpty()) {
                if(verified.contains(skill) && scoreBySkill.containsKey(skill)) {
                    var score=scoreBySkill.get(skill);
                    boolean coverage=skill==Skill.LISTENING || skill==Skill.READING ? skillItems.size()==40 : writingOrSpeakingCoverage(skill,skillItems);
                    value=new BandMeasurement(skill.name(),score.band(),score.gradingSource().name(),"MEASURED","rubric-v1",coverage?null:"INSUFFICIENT_COVERAGE");
                } else if(attempt.getAttemptType()==AttemptType.PLACEMENT && (skill==Skill.LISTENING || skill==Skill.READING)) {
                    int correct=(int)skillItems.stream().filter(i->gradedByItem.containsKey(i.id()) && Boolean.TRUE.equals(gradedByItem.get(i.id()).correct())).count();
                    value=new BandMeasurement(skill.name(),PlacementBandCalculator.bandForCorrect(skill,correct,skillItems.size()),
                            "AUTO","MEASURED",version,skillItems.size()==40?null:"INSUFFICIENT_COVERAGE");
                } else if(attempt.getAttemptType()==AttemptType.PLACEMENT && skill==Skill.WRITING) {
                    boolean measured=skillItems.stream().allMatch(i -> jobByItem.containsKey(i.id())
                            && "COMPLETED".equals(jobByItem.get(i.id()).status()) && "MEASURED".equals(jobByItem.get(i.id()).measurementQuality())
                            && jobByItem.get(i.id()).band()!=null);
                    boolean fallback=skillItems.stream().anyMatch(i -> jobByItem.containsKey(i.id()) && "FALLBACK".equals(jobByItem.get(i.id()).measurementQuality()));
                    if(measured) {
                        double weighted=0,weights=0;
                        for(var item:skillItems) {
                            double weight=AnswerSnapshot.parse(json,item.answerSnapshot()).map(a->PlacementBandCalculator.writingTaskWeight(a.answerSpec()==null?null:a.answerSpec().get("task"))).orElse(1.0);
                            weighted+=jobByItem.get(item.id()).band().doubleValue()*weight;weights+=weight;
                        }
                        value=new BandMeasurement(skill.name(),weighted/weights,"AI","MEASURED",version,
                                writingOrSpeakingCoverage(skill,skillItems)?null:"INSUFFICIENT_COVERAGE");
                    } else value=new BandMeasurement(skill.name(),null,"AI",fallback?"FALLBACK":"NOT_ASSESSED",version,fallback?"FALLBACK":"NOT_ASSESSED");
                }
            }
            snapshot.add(value);
        }
        var eligible=snapshot.stream().filter(BandMeasurement::eligible).toList();
        snapshot.add(new BandMeasurement("OVERALL",eligible.size()==4?PlacementBandCalculator.overallBand(eligible.stream().map(BandMeasurement::band).toList()):null,
                "MIXED",eligible.size()==4?"MEASURED":"NOT_ASSESSED","rubric-v1",eligible.size()==4?null:"INCOMPLETE_SKILL_COVERAGE"));
        store.freeze(result.id(),snapshot);
    }
    private boolean writingOrSpeakingCoverage(Skill skill,List<AttemptItem> items) {
        if(skill==Skill.SPEAKING) return !items.isEmpty();
        Set<Object> tasks=new HashSet<>();
        for(var item:items) AnswerSnapshot.parse(json,item.answerSnapshot()).ifPresent(a->{ if(a.answerSpec()!=null) tasks.add(a.answerSpec().get("task")); });
        return items.size()==2 && tasks.containsAll(Set.of("TASK_1","TASK_2"));
    }
}
