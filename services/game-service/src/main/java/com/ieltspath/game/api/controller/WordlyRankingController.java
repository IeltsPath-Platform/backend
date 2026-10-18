package com.ieltspath.game.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.game.application.result.WordlyRankingResult;
import com.ieltspath.game.application.usecase.GetWordlyRankingUseCase;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games/matches")
public class WordlyRankingController {
    private final CurrentUserProvider user;
    private final GetWordlyRankingUseCase rankings;

    public WordlyRankingController(CurrentUserProvider user, GetWordlyRankingUseCase rankings) {
        this.user = user;
        this.rankings = rankings;
    }

    @GetMapping("/{matchId}/ranking")
    public WordlyRankingResult get(@PathVariable("matchId") UUID matchId) {
        return rankings.execute(matchId, user.requireUserId());
    }
}
