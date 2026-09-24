package com.group01.game.api.dto.response;

import com.group01.game.application.result.GameRoomResult;

import java.time.Instant;
import java.util.UUID;

public record GameRoomMemberResponse(UUID userId, String role, String status, Instant joinedAt) {
    public static GameRoomMemberResponse from(GameRoomResult.MemberResult result) {
        return new GameRoomMemberResponse(result.userId(), result.role(), result.status(), result.joinedAt());
    }
}
