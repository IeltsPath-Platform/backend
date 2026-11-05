package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.StudyTimezones;
import com.ieltspath.learning.application.result.StudyTimeStatisticsResult;
import com.ieltspath.learning.application.result.StudyTimeStatisticsResult.Day;
import com.ieltspath.learning.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GetStudyTimeStatisticsUseCase {
    private final StudySessionRepository sessions;
    private final StudyTimezones timezones;
    private final Clock clock;

    @Transactional(readOnly=true)
    public StudyTimeStatisticsResult execute(UUID user,LocalDate from,LocalDate to,String grain,UUID courseId) {
        if(from==null || to==null || to.isBefore(from) || ChronoUnit.DAYS.between(from,to)>=366)
            throw new IllegalArgumentException("Study-time range must contain 1 to 366 days");
        if(!Set.of("DAY","WEEK","MONTH").contains(grain)) throw new IllegalArgumentException("grain must be DAY, WEEK or MONTH");
        var zone=timezones.zoneOf(user);
        var now=clock.instant();
        LocalDate today=now.atZone(zone).toLocalDate(), dataFrom=sessions.trackingStartedAt().atZone(zone).toLocalDate();
        String scope=courseId==null ? "ALL" : courseId.toString();
        var totals=new HashMap<LocalDate,StudySessionRepository.Daily>();
        sessions.daily(user,from,to,scope).forEach(d->totals.put(d.date(),d));
        var goals=new TreeMap<LocalDate,StudySessionRepository.Goal>();
        if(courseId==null) sessions.goals(user,from,to).forEach(g->goals.put(g.effectiveDate(),g));
        var days=new ArrayList<Day>();
        for(LocalDate date=from; !date.isAfter(to); date=date.plusDays(1)) {
            var total=totals.get(date);
            // The recorded date remains authoritative after a profile timezone change.
            boolean untracked=date.isBefore(dataFrom) && total==null, future=date.isAfter(today) && total==null;
            var goal=goals.floorEntry(date);
            Long planned=untracked || goal==null || goal.getValue().minutes()==null ? null : goal.getValue().minutes()*60L;
            Long actual=untracked || future ? null : total==null ? 0L : total.milliseconds()/1000;
            String status=untracked ? "NOT_TRACKED" : future ? "FUTURE" : date.equals(today) || date.equals(dataFrom) ? "PARTIAL_DAY" : "COMPLETE";
            days.add(new Day(date,total==null ? zone.getId() : total.timezone(),actual,planned,status));
        }
        return new StudyTimeStatisticsResult("AVAILABLE",now,dataFrom,"RECORDED_LOCAL_DATE",scope,
                grain.equals("DAY") ? List.copyOf(days) : aggregate(days,grain,totals));
    }
    private List<Day> aggregate(List<Day> days,String grain,Map<LocalDate,StudySessionRepository.Daily> totals) {
        var groups=new LinkedHashMap<LocalDate,List<Day>>();
        for(Day day:days) {
            LocalDate key=grain.equals("WEEK") ? day.date().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) : day.date().withDayOfMonth(1);
            groups.computeIfAbsent(key,k->new ArrayList<>()).add(day);
        }
        var result=new ArrayList<Day>();
        groups.forEach((date,values)-> {
            Long actualMs=null,planned=null;
            Set<String> zones=new HashSet<>(), statuses=new HashSet<>();
            for(Day day:values) {
                if(day.actualSeconds()!=null) {
                    var total=totals.get(day.date());
                    actualMs=(actualMs==null?0:actualMs)+(total==null?0:total.milliseconds());
                }
                if(day.plannedSeconds()!=null) planned=(planned==null?0:planned)+day.plannedSeconds();
                zones.add(day.timezone()); statuses.add(day.status());
            }
            String status=statuses.size()==1 ? statuses.iterator().next() : "PARTIAL_DAY";
            Long actual=actualMs==null ? null : actualMs/1000;
            result.add(new Day(date,zones.size()==1 ? zones.iterator().next() : "MULTIPLE",actual,planned,status));
        });
        return List.copyOf(result);
    }
}
