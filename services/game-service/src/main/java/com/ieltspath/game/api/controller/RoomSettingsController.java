package com.ieltspath.game.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.game.api.dto.request.UpdateRoomSettingsRequest;
import com.ieltspath.game.api.dto.response.GameRoomResponse;
import com.ieltspath.game.application.usecase.UpdateRoomSettingsUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/games/rooms")
public class RoomSettingsController {
    private final CurrentUserProvider user;
    private final UpdateRoomSettingsUseCase update;

    public RoomSettingsController(CurrentUserProvider user, UpdateRoomSettingsUseCase update) {
        this.user = user;
        this.update = update;
    }

    @PutMapping("/{roomId}/settings")
    public GameRoomResponse update(@PathVariable("roomId") UUID roomId,
                                   @Valid @RequestBody UpdateRoomSettingsRequest request) {
        return GameRoomResponse.from(update.execute(roomId, user.requireUserId(), request.initialLives(), request.wordLimit(),
                request.timeEnabled(), request.questionCount(), request.secondsPerQuestion(),
                new com.ieltspath.game.domain.vo.WordFilter(request.cefrLevel(), request.topicId())));
    }
}
