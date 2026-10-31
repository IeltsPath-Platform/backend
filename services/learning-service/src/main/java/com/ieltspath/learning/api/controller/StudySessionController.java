package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.request.*;
import com.ieltspath.learning.application.result.*;
import com.ieltspath.learning.application.usecase.*;
import com.ieltspath.learning.domain.aggregate.StudySession.Interval;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class StudySessionController {
    private final CurrentUserProvider currentUser;
    private final StartStudySessionUseCase start;
    private final RecordStudyHeartbeatUseCase heartbeat;
    private final GetStudyTimeStatisticsUseCase statistics;

    @PostMapping("/study-sessions")
    public StudySessionResult start(@Valid @RequestBody StartStudySessionRequest request) {
        return start.execute(currentUser.requireUserId(),request.clientSessionId(),request.sourceType(),request.sourceId(),request.takeover());
    }
    @PostMapping("/study-sessions/{id}/heartbeats")
    public StudyHeartbeatResult heartbeat(@PathVariable("id") UUID id,@Valid @RequestBody StudyHeartbeatRequest request) {
        return record(id,request,false);
    }
    @PostMapping("/study-sessions/{id}/close")
    public StudyHeartbeatResult close(@PathVariable("id") UUID id,@Valid @RequestBody StudyHeartbeatRequest request) {
        return record(id,request,true);
    }
    @GetMapping("/statistics/study-time")
    public StudyTimeStatisticsResult statistics(@RequestParam("from") LocalDate from,@RequestParam("to") LocalDate to,
            @RequestParam(name="grain",defaultValue="DAY") String grain,@RequestParam(name="courseId",required=false) UUID courseId) {
        return statistics.execute(currentUser.requireUserId(),from,to,grain,courseId);
    }
    private StudyHeartbeatResult record(UUID id,StudyHeartbeatRequest request,boolean close) {
        return heartbeat.execute(currentUser.requireUserId(),id,request.epoch(),request.sequence(),
                request.intervals().stream().map(i->new Interval(i.startOffsetMs(),i.endOffsetMs())).toList(),close);
    }
}
