package com.ieltspath.game.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.game.api.dto.request.CreateRocketRoomRequest;
import com.ieltspath.game.api.dto.request.SubmitRocketAnswerRequest;
import com.ieltspath.game.api.dto.response.GameRoomResponse;
import com.ieltspath.game.application.result.RocketGameplayResults.AnswerAck;
import com.ieltspath.game.application.result.RocketGameplayResults.Current;
import com.ieltspath.game.application.usecase.CreateWordlyRoomUseCase;
import com.ieltspath.game.application.usecase.RocketGameplayUseCase;
import com.ieltspath.game.domain.vo.RocketRoomSettings;
import com.ieltspath.game.domain.vo.WordlyRoomSettings;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Rocket rooms and rounds. Ready, start and the other room routes are shared under /api/games/rooms. */
@RestController
@RequestMapping("/api/games/rocket")
public class RocketGameController {
    private final CurrentUserProvider currentUser;
    private final CreateWordlyRoomUseCase rooms;
    private final RocketGameplayUseCase gameplay;

    public RocketGameController(CurrentUserProvider currentUser, CreateWordlyRoomUseCase rooms, RocketGameplayUseCase gameplay) {
        this.currentUser = currentUser;
        this.rooms = rooms;
        this.gameplay = gameplay;
    }

    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public GameRoomResponse createRoom(@Valid @RequestBody CreateRocketRoomRequest request) {
        RocketRoomSettings settings = request.playerMode() == WordlyRoomSettings.PlayerMode.SOLO
                ? RocketRoomSettings.solo(request.questionCount())
                : RocketRoomSettings.multiplayer(request.questionCount(),
                        request.initialLives() == null ? RocketRoomSettings.DEFAULT_LIVES : request.initialLives(),
                        request.secondsPerQuestion() == null ? RocketRoomSettings.DEFAULT_SECONDS : request.secondsPerQuestion());
        return GameRoomResponse.from(rooms.open(currentUser.requireUserId(), settings));
    }

    @GetMapping("/matches/{matchId}/rounds/current")
    public Current current(@PathVariable UUID matchId) {
        return gameplay.current(matchId, currentUser.requireUserId());
    }

    @PostMapping("/matches/{matchId}/rounds/{roundId}/answers")
    public AnswerAck answer(@PathVariable UUID matchId, @PathVariable UUID roundId,
            @Valid @RequestBody SubmitRocketAnswerRequest request) {
        return gameplay.answer(matchId, roundId, currentUser.requireUserId(), request.requestId(), request.optionId());
    }
}
