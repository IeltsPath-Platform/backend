package com.ieltspath.game.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.game.api.dto.response.GameMatchResponse;
import com.ieltspath.game.application.usecase.GetGameMatchStateUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/games/matches")
public class GameMatchController {
    private final CurrentUserProvider currentUserProvider;
    private final GetGameMatchStateUseCase getMatchState;

    public GameMatchController(CurrentUserProvider currentUserProvider, GetGameMatchStateUseCase getMatchState) {
        this.currentUserProvider = currentUserProvider;
        this.getMatchState = getMatchState;
    }

    @GetMapping("/{matchId}")
    public GameMatchResponse get(@PathVariable UUID matchId) {
        return GameMatchResponse.from(getMatchState.execute(matchId, currentUserProvider.requireUserId()));
    }
}
