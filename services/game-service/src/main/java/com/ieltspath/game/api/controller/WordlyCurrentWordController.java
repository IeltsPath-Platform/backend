package com.ieltspath.game.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.game.application.result.CurrentWordResult;
import com.ieltspath.game.application.usecase.GetCurrentWordUseCase;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/games/matches")
public class WordlyCurrentWordController {
    private final CurrentUserProvider user;
    private final GetCurrentWordUseCase current;

    public WordlyCurrentWordController(CurrentUserProvider user, GetCurrentWordUseCase current) {
        this.user = user;
        this.current = current;
    }

    @GetMapping("/{matchId}/words/current")
    public Response get(@PathVariable("matchId") UUID matchId) {
        return Response.from(current.execute(matchId, user.requireUserId()));
    }

    public record Response(
            UUID playerWordId,
            int wordNumber,
            Integer wordLimit,
            String meaning,
            int columns,
            int maxAttempts,
            int attemptsUsed,
            boolean timeEnabled,
            java.time.Instant deadlineAt,
            String wordStatus,
            Player player,
            java.util.List<CurrentWordResult.Guess> guesses,
            java.util.Map<String, String> keyboard,
            com.ieltspath.game.api.dto.response.WordlyHintResponse hint) {
        static Response from(CurrentWordResult r) {
            return new Response(
                    r.playerWordId(),
                    r.wordNumber(),
                    r.wordLimit(),
                    r.meaning(),
                    r.columns(),
                    r.maxAttempts(),
                    r.attemptsUsed(),
                    r.timeEnabled(),
                    r.deadlineAt(),
                    r.wordStatus(),
                    new Player(
                            r.playerStatus(),
                            r.livesRemaining(),
                            r.totalScore(),
                            r.currentStreak(),
                            r.bestStreak()),
                    r.guesses(),
                    r.getKeyboard(),
                    r.hint() == null
                            ? null
                            : com.ieltspath.game.api.dto.response.WordlyHintResponse.from(
                                    r.hint()));
        }
    }

    public record Player(
            String status,
            int livesRemaining,
            long totalScore,
            int currentStreak,
            int bestStreak) {}
}
