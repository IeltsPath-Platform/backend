package com.ieltspath.game.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.game.api.dto.request.CreateWordlyRoomRequest;
import com.ieltspath.game.api.dto.response.GameRoomResponse;
import com.ieltspath.game.application.command.CreateWordlyRoomCommand;
import com.ieltspath.game.application.usecase.CreateWordlyRoomUseCase;
import com.ieltspath.game.domain.vo.WordlyRoomSettings;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games/wordly/rooms")
public class WordlyRoomController {
    private final CurrentUserProvider currentUser;
    private final CreateWordlyRoomUseCase createRoom;

    public WordlyRoomController(CurrentUserProvider currentUser, CreateWordlyRoomUseCase createRoom) {
        this.currentUser = currentUser;
        this.createRoom = createRoom;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameRoomResponse create(@Valid @RequestBody CreateWordlyRoomRequest request) {
        var settings = new WordlyRoomSettings(request.playerMode(), request.initialLives(), request.playMode(), request.wordLimit(), request.timeEnabled() == null || request.timeEnabled(),
                new com.ieltspath.game.domain.vo.WordFilter(request.cefrLevel(), request.topicId()));
        return GameRoomResponse.from(createRoom.execute(new CreateWordlyRoomCommand(currentUser.requireUserId(), settings)));
    }
}
