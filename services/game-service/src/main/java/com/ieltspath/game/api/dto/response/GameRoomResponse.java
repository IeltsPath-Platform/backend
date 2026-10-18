package com.ieltspath.game.api.dto.response;

import com.ieltspath.game.application.result.GameRoomResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GameRoomResponse(UUID id, String roomCode, UUID hostUserId, String gameType,
                               String learningDomain, String mode, int maxPlayers, String status,
                               Instant createdAt, List<GameRoomMemberResponse> members,
                               List<GameItemResponse> items, String playerMode, SettingsResponse settings, UUID activeMatchId) {
    public record SettingsResponse(int initialLives, Integer wordTimeLimitSeconds, String playMode, Integer wordLimit, boolean timeEnabled) {}
    public GameRoomResponse {
        members = List.copyOf(members);
        items = List.copyOf(items);
    }

    @com.fasterxml.jackson.annotation.JsonProperty("canStart")
    public boolean canStart() {
        var active = members.stream().filter(m -> !"LEFT".equals(m.status())).toList();
        return "WAITING".equals(status) && active.size() >= ("SOLO".equals(playerMode) ? 1 : 2)
                && active.stream().allMatch(m -> "READY".equals(m.status()));
    }

    @com.fasterxml.jackson.annotation.JsonProperty("message")
    public String message() {
        if (!"CLOSED".equals(status)) return null;
        String name = members.stream().filter(m -> m.userId().equals(hostUserId))
                .map(m -> m.displayName() == null ? hostUserId.toString() : m.displayName()).findFirst().orElse(hostUserId.toString());
        return "Chủ phòng " + name + " đã đóng phòng";
    }

    public static GameRoomResponse from(GameRoomResult result) {
        return new GameRoomResponse(result.id(), result.roomCode(), result.hostUserId(), result.gameType(),
                result.learningDomain(), result.mode(), result.maxPlayers(), result.status(), result.createdAt(),
                result.members().stream().map(GameRoomMemberResponse::from).toList(),
                result.items().stream().map(GameItemResponse::from).toList(), result.playerMode(),
                result.settings() == null ? null : new SettingsResponse(result.settings().initialLives(),
                        result.settings().wordTimeLimitSeconds(), result.settings().playMode(), result.settings().wordLimit(), result.settings().timeEnabled()),result.activeMatchId());
    }
}
