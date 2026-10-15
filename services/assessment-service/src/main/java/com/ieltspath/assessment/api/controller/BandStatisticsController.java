package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.usecase.GetBandHistoryUseCase;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/assessments/me/statistics")
public class BandStatisticsController {
    private final CurrentUserProvider currentUser;
    private final GetBandHistoryUseCase history;
    public BandStatisticsController(CurrentUserProvider currentUser,GetBandHistoryUseCase history) { this.currentUser=currentUser;this.history=history; }
    @GetMapping("/bands")
    public GetBandHistoryUseCase.Result bands(@RequestParam("from") LocalDate from,@RequestParam("to") LocalDate to,
            @RequestParam(name="seriesKind",defaultValue="PLACEMENT") String seriesKind,
            @RequestParam(name="skill",defaultValue="OVERALL") String skill,
            @RequestParam(name="cursor",required=false) String cursor,
            @RequestParam(name="limit",defaultValue="100") int limit,
            @RequestParam(name="timezone",defaultValue="Asia/Ho_Chi_Minh") String timezone) {
        return history.execute(currentUser.requireUserId(),from,to,seriesKind,skill,cursor,limit,timezone);
    }
    @GetMapping("/study-context")
    public GetBandHistoryUseCase.StudyContext context(@RequestParam("sourceType") String sourceType,
            @RequestParam("sourceId") UUID sourceId) {
        return history.context(currentUser.requireUserId(),sourceType,sourceId);
    }
}
