package com.ieltspath.game.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
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

    public SoloGameplayController(CurrentUserProvider user, SoloGameplayUseCase gameplay) {
        this.user = user;
        this.gameplay = gameplay;
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
    public ResponseEntity<Map<String, Object>> leave(@PathVariable("matchId") UUID matchId) {
        var reply = gameplay.leave(matchId, user.requireUserId());
        return ResponseEntity.status(reply.status()).body(reply.body());
    }

    @GetMapping("/{matchId}/result")
    public Map<String, Object> result(@PathVariable("matchId") UUID matchId) {
        return gameplay.result(matchId, user.requireUserId());
    }

    public record GuessRequest(
            @NotNull UUID requestId,
            @NotNull UUID playerWordId,
            @NotBlank @Size(max = 100) String guess) {}
}
