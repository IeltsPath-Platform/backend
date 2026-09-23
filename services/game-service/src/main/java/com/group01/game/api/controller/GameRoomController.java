package com.group01.game.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.game.api.dto.request.CreateGameRoomRequest;
import com.group01.game.api.dto.request.ReadyStatusRequest;
import com.group01.game.application.command.CreateGameRoomCommand;
import com.group01.game.application.result.GameRoomResult;
import com.group01.game.application.usecase.ChangeGameRoomMembershipUseCase;
import com.group01.game.application.usecase.CreateGameRoomUseCase;
import com.group01.game.application.usecase.CreateWebSocketTicketUseCase;
import com.group01.game.application.usecase.GetGameRoomUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/games/rooms")
public class GameRoomController {
    private final CurrentUserProvider currentUserProvider;
    private final CreateGameRoomUseCase createRoom;
    private final GetGameRoomUseCase getRoom;
    private final ChangeGameRoomMembershipUseCase membership;
    private final CreateWebSocketTicketUseCase createTicket;

    public GameRoomController(CurrentUserProvider currentUserProvider, CreateGameRoomUseCase createRoom,
                              GetGameRoomUseCase getRoom, ChangeGameRoomMembershipUseCase membership,
                              CreateWebSocketTicketUseCase createTicket) {
        this.currentUserProvider = currentUserProvider;
        this.createRoom = createRoom;
        this.getRoom = getRoom;
        this.membership = membership;
        this.createTicket = createTicket;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameRoomResult create(@Valid @RequestBody CreateGameRoomRequest request) {
        return createRoom.execute(new CreateGameRoomCommand(currentUserProvider.requireUserId(), request.gameType(),
                request.learningDomain(), request.mode(), request.maxPlayers(), request.contentIds()));
    }

    @GetMapping("/{roomId}")
    public GameRoomResult get(@PathVariable("roomId") UUID roomId) {
        return getRoom.byId(roomId, currentUserProvider.requireUserId());
    }

    @GetMapping("/code/{roomCode}")
    public GameRoomResult getByCode(@PathVariable("roomCode") String roomCode) {
        return getRoom.byCode(roomCode, currentUserProvider.requireUserId());
    }

    @PostMapping("/{roomId}/join")
    public GameRoomResult join(@PathVariable("roomId") UUID roomId) {
        return membership.join(roomId, currentUserProvider.requireUserId());
    }

    @PostMapping("/code/{roomCode}/join")
    public GameRoomResult joinByCode(@PathVariable("roomCode") String roomCode) {
        return membership.joinByCode(roomCode, currentUserProvider.requireUserId());
    }

    @PostMapping("/{roomId}/ready")
    public GameRoomResult ready(@PathVariable("roomId") UUID roomId,
                                @Valid @RequestBody ReadyStatusRequest request) {
        return membership.ready(roomId, currentUserProvider.requireUserId(), request.ready());
    }

    @PostMapping("/{roomId}/leave")
    public GameRoomResult leave(@PathVariable("roomId") UUID roomId) {
        return membership.leave(roomId, currentUserProvider.requireUserId());
    }

    @PostMapping("/{roomId}/close")
    public GameRoomResult close(@PathVariable("roomId") UUID roomId) {
        return membership.close(roomId, currentUserProvider.requireUserId());
    }

    @PostMapping("/{roomId}/websocket-ticket")
    public CreateWebSocketTicketUseCase.TicketResult websocketTicket(@PathVariable("roomId") UUID roomId) {
        return createTicket.execute(roomId, currentUserProvider.requireUserId());
    }
}
