package com.ieltspath.library.application.service;

import com.ieltspath.library.application.port.StudyProgressReader;
import com.ieltspath.library.application.result.StudyAttemptResult.*;
import com.ieltspath.library.domain.study.UnitUnlockRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class StudyProgressService {
    private final StudyProgressReader reader;
    private final Clock clock;
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public List<TopicProgress> execute(UUID user,List<UUID> topics) {
        return read(user,topics,null);
    }
    public List<TopicProgress> afterCompletion(UUID user,List<UUID> topics,UUID completedAttempt) {
        return read(user,topics,completedAttempt);
    }
    private List<TopicProgress> read(UUID user,List<UUID> topics,UUID excludedAttempt) {
        if(topics==null || topics.isEmpty() || topics.size()>100 || topics.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException("topicIds cần 1–100 UUID.");
        var snapshot=reader.read(user,topics,clock.instant());
        var completed=snapshot.progress().stream().collect(Collectors.toMap(StudyProgressReader.Progress::unitId,Function.identity()));
        var relevant=snapshot.attempts().stream().filter(a->!a.id().equals(excludedAttempt)).toList();
        var attempts=relevant.stream().collect(Collectors.toMap(StudyProgressReader.ActiveAttempt::unitId,Function.identity()));
        var active=relevant.stream().filter(a->a.mode().equals("FIRST_COMPLETION")).map(StudyProgressReader.ActiveAttempt::unitId).collect(Collectors.toSet());
        var byTopic=snapshot.units().stream().collect(Collectors.groupingBy(StudyProgressReader.PublishedUnit::topicId));
        var result=new ArrayList<TopicProgress>();
        for(UUID topic:new LinkedHashSet<>(topics)) {
            var units=byTopic.getOrDefault(topic,List.of());var ids=units.stream().map(StudyProgressReader.PublishedUnit::id).toList();
            var statuses=UnitUnlockRules.statuses(ids,completed.keySet(),active);var progress=new ArrayList<UnitProgress>();UUID current=null;
            for(int i=0;i<units.size();i++) {
                UUID id=ids.get(i);var p=completed.get(id);var a=attempts.get(id);String status=statuses.get(i);
                progress.add(new UnitProgress(id,status,i==0?null:ids.get(i-1),a==null?null:a.id(),a==null?0:a.completedSteps(),a==null?0:a.totalSteps(),p==null?null:p.firstCompletedAt(),p==null?null:p.lastCompletedAt(),p==null?null:p.bestAccuracyPercent()));
                if(current==null && status.equals("IN_PROGRESS"))current=id;
            }
            if(current==null)current=progress.stream().filter(p->p.status().equals("AVAILABLE")).map(UnitProgress::unitId).findFirst().orElse(null);
            int version=snapshot.versions().getOrDefault(topic,0);
            result.add(new TopicProgress(topic,version,units.size(),(int)progress.stream().filter(p->p.status().equals("COMPLETED")).count(),current,progress,clock.instant()));
        }
        return result;
    }
}
