package com.ieltspath.game.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.game.application.usecase.RocketGameplayUseCase;
import com.ieltspath.game.application.usecase.SoloGameplayUseCase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/games/matches")
public class SoloGameplayController {
    private final CurrentUserProvider user;
    private final SoloGameplayUseCase gameplay;
    private final RocketGameplayUseCase rocket;

    public SoloGameplayController(CurrentUserProvider user, SoloGameplayUseCase gameplay, RocketGameplayUseCase rocket) {
        this.user = user;
        this.gameplay = gameplay;
        this.rocket = rocket;
    }

    @PostMapping("/{matchId}/guesses")
    public ResponseEntity<Map<String, Object>> guess(
            @PathVariable("matchId") UUID matchId, @Valid @RequestBody GuessRequest request) {
        var reply =
                gameplay.submit(
                        matchId,
                        request.playerWordId(),
                        user.requireUserId(),
                        request.requestId(),
                        request.guess());
        return ResponseEntity.status(reply.status()).body(reply.body());
    }

    @PostMapping("/{matchId}/leave")
    public ResponseEntity<?> leave(@PathVariable("matchId") UUID matchId) {
        // Leave and result are shared match routes; Rocket matches are served by the Rocket use case.
        if (rocket.isRocket(matchId)) return ResponseEntity.ok(rocket.leave(matchId, user.requireUserId()));
        var reply = gameplay.leave(matchId, user.requireUserId());
        return ResponseEntity.status(reply.status()).body(reply.body());
    }

    @GetMapping("/{matchId}/result")
    public Object result(@PathVariable("matchId") UUID matchId) {
        if (rocket.isRocket(matchId)) return rocket.result(matchId, user.requireUserId());
        return gameplay.result(matchId, user.requireUserId());
    }

    public record GuessRequest(
            @NotNull UUID requestId,
            @NotNull UUID playerWordId,
            @NotBlank @Size(max = 100) String guess) {}
}
