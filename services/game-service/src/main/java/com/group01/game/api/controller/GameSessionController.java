package com.group01.game.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.game.api.dto.request.StartGameSessionRequest;
import com.group01.game.api.dto.request.SubmitGameAnswerRequest;
import com.group01.game.api.dto.response.GameAnswerResponse;
import com.group01.game.api.dto.response.GameHistoryResponse;
import com.group01.game.api.dto.response.GameSessionResponse;
import com.group01.game.application.command.StartGameSessionCommand;
import com.group01.game.application.usecase.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/games/sessions")
public class GameSessionController {
    private final CurrentUserProvider currentUserProvider;
    private final StartGameSessionUseCase startSession;
    private final SubmitGameAnswerUseCase submitAnswer;
    private final GetGameSessionUseCase getSession;
    private final GetGameHistoryUseCase getHistory;
    private final AbandonGameSessionUseCase abandonSession;

    public GameSessionController(CurrentUserProvider currentUserProvider, StartGameSessionUseCase startSession,
                                 SubmitGameAnswerUseCase submitAnswer, GetGameSessionUseCase getSession,
                                 GetGameHistoryUseCase getHistory, AbandonGameSessionUseCase abandonSession) {
        this.currentUserProvider = currentUserProvider;
        this.startSession = startSession;
        this.submitAnswer = submitAnswer;
        this.getSession = getSession;
        this.getHistory = getHistory;
        this.abandonSession = abandonSession;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameSessionResponse start(@Valid @RequestBody StartGameSessionRequest request) {
        return GameSessionResponse.from(startSession.execute(new StartGameSessionCommand(currentUserProvider.requireUserId(),
                request.gameType(), request.learningDomain(), request.mode(), request.topicId(), request.contentIds())));
    }

    @PostMapping("/{sessionId}/answers/{itemSequence}")
    public GameAnswerResponse answer(@PathVariable UUID sessionId, @PathVariable int itemSequence,
                                   @Valid @RequestBody SubmitGameAnswerRequest request) {
        return GameAnswerResponse.from(submitAnswer.execute(sessionId, currentUserProvider.requireUserId(), itemSequence,
                request.responsePayload(), request.durationMilliseconds()));
    }

    @GetMapping("/{sessionId}")
    public GameSessionResponse get(@PathVariable UUID sessionId) {
        return GameSessionResponse.from(getSession.execute(sessionId, currentUserProvider.requireUserId()));
    }

    @GetMapping
    public GameHistoryResponse history(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return GameHistoryResponse.from(getHistory.execute(currentUserProvider.requireUserId(), page, size));
    }

    @PostMapping("/{sessionId}/abandon")
    public GameSessionResponse abandon(@PathVariable UUID sessionId) {
        return GameSessionResponse.from(abandonSession.execute(sessionId, currentUserProvider.requireUserId()));
    }
}
