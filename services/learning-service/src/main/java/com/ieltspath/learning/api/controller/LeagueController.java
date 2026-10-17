package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.usecase.GetLeagueUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/learning/me/league")
@RequiredArgsConstructor
public class LeagueController {
    private final CurrentUserProvider currentUser;
    private final GetLeagueUseCase league;

    /** The learner's tier, this week's table, the promotion and demotion zones and when the week closes. */
    @GetMapping
    public GetLeagueUseCase.LeagueView view() {
        return league.view(currentUser.requireUserId());
    }

    /** The weeks the learner played, newest first. */
    @GetMapping("/history")
    public List<GetLeagueUseCase.HistoryEntry> history(@RequestParam(name = "weeks", defaultValue = "8") int weeks) {
        return league.history(currentUser.requireUserId(), weeks);
    }
}
