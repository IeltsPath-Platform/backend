package com.group01.game.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.game.api.dto.request.CreateGameRoomRequest;
import com.group01.game.api.dto.request.ReadyStatusRequest;
import com.group01.game.api.dto.response.GameRoomResponse;
import com.group01.game.api.dto.response.GameTicketResponse;
import com.group01.game.application.command.CreateGameRoomCommand;
import com.group01.game.application.usecase.ChangeGameRoomMembershipUseCase;
import com.group01.game.application.usecase.CreateGameRoomUseCase;
import com.group01.game.application.usecase.CreateWebSocketTicketUseCase;
import com.group01.game.application.usecase.GetGameRoomUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
    public GameRoomResponse create(@Valid @RequestBody CreateGameRoomRequest request) {
        return GameRoomResponse.from(createRoom.execute(new CreateGameRoomCommand(currentUserProvider.requireUserId(), request.gameType(),
                request.learningDomain(), request.mode(), request.maxPlayers(), request.contentIds())));
    }

    @GetMapping("/{roomId}")
    public GameRoomResponse get(@PathVariable("roomId") UUID roomId) {
        return GameRoomResponse.from(getRoom.byId(roomId, currentUserProvider.requireUserId()));
    }

    @GetMapping("/code/{roomCode}")
    public GameRoomResponse getByCode(@PathVariable("roomCode") String roomCode) {
        return GameRoomResponse.from(getRoom.byCode(roomCode, currentUserProvider.requireUserId()));
    }

    @PostMapping("/{roomId}/join")
    public GameRoomResponse join(@PathVariable("roomId") UUID roomId) {
        return GameRoomResponse.from(membership.join(roomId, currentUserProvider.requireUserId()));
    }

    @PostMapping("/code/{roomCode}/join")
    public GameRoomResponse joinByCode(@PathVariable("roomCode") String roomCode) {
        return GameRoomResponse.from(membership.joinByCode(roomCode, currentUserProvider.requireUserId()));
    }

    @PostMapping("/{roomId}/ready")
    public GameRoomResponse ready(@PathVariable("roomId") UUID roomId,
                                @Valid @RequestBody ReadyStatusRequest request) {
        return GameRoomResponse.from(membership.ready(roomId, currentUserProvider.requireUserId(), request.ready()));
    }

    @PostMapping("/{roomId}/leave")
    public GameRoomResponse leave(@PathVariable("roomId") UUID roomId) {
        return GameRoomResponse.from(membership.leave(roomId, currentUserProvider.requireUserId()));
    }

    @PostMapping("/{roomId}/close")
    public GameRoomResponse close(@PathVariable("roomId") UUID roomId) {
        return GameRoomResponse.from(membership.close(roomId, currentUserProvider.requireUserId()));
    }

    @PostMapping("/{roomId}/websocket-ticket")
    public GameTicketResponse websocketTicket(@PathVariable("roomId") UUID roomId) {
        return GameTicketResponse.from(createTicket.execute(roomId, currentUserProvider.requireUserId()));
    }
}
