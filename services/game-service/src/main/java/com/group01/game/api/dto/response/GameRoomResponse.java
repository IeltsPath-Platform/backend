package com.group01.game.api.dto.response;

import com.group01.game.application.result.GameRoomResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GameRoomResponse(UUID id, String roomCode, UUID hostUserId, String gameType,
                               String learningDomain, String mode, int maxPlayers, String status,
                               Instant createdAt, List<GameRoomMemberResponse> members,
                               List<GameItemResponse> items) {
    public GameRoomResponse {
        members = List.copyOf(members);
        items = List.copyOf(items);
    }

    public static GameRoomResponse from(GameRoomResult result) {
        return new GameRoomResponse(result.id(), result.roomCode(), result.hostUserId(), result.gameType(),
                result.learningDomain(), result.mode(), result.maxPlayers(), result.status(), result.createdAt(),
                result.members().stream().map(GameRoomMemberResponse::from).toList(),
                result.items().stream().map(GameItemResponse::from).toList());
    }
}
