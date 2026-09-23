package com.group01.game.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.game.application.command.StartGameSessionCommand;
import com.group01.game.application.result.GameAnswerResult;
import com.group01.game.application.result.GameSessionResult;
import com.group01.game.application.usecase.AbandonGameSessionUseCase;
import com.group01.game.application.usecase.GetGameHistoryUseCase;
import com.group01.game.application.usecase.GetGameSessionUseCase;
import com.group01.game.application.usecase.StartGameSessionUseCase;
import com.group01.game.application.usecase.SubmitGameAnswerUseCase;
import com.group01.game.api.dto.request.StartGameSessionRequest;
import com.group01.game.api.dto.request.SubmitGameAnswerRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    public GameSessionResult start(@Valid @RequestBody StartGameSessionRequest request) {
        return startSession.execute(new StartGameSessionCommand(currentUserProvider.requireUserId(),
                request.gameType(), request.learningDomain(), request.mode(), request.topicId(), request.contentIds()));
    }

    @PostMapping("/{sessionId}/answers/{itemSequence}")
    public GameAnswerResult answer(@PathVariable UUID sessionId, @PathVariable int itemSequence,
                                   @Valid @RequestBody SubmitGameAnswerRequest request) {
        return submitAnswer.execute(sessionId, currentUserProvider.requireUserId(), itemSequence,
                request.responsePayload(), request.durationMilliseconds());
    }

    @GetMapping("/{sessionId}")
    public GameSessionResult get(@PathVariable UUID sessionId) {
        return getSession.execute(sessionId, currentUserProvider.requireUserId());
    }

    @GetMapping
    public GetGameHistoryUseCase.HistoryResult history(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return getHistory.execute(currentUserProvider.requireUserId(), page, size);
    }

    @PostMapping("/{sessionId}/abandon")
    public GameSessionResult abandon(@PathVariable UUID sessionId) {
        return abandonSession.execute(sessionId, currentUserProvider.requireUserId());
    }
}
