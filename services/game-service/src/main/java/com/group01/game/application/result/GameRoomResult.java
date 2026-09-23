package com.group01.game.application.result;

import com.group01.game.domain.aggregate.GameRoom;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GameRoomResult(UUID id, String roomCode, UUID hostUserId, String gameType,
                             String learningDomain, String mode, int maxPlayers, String status,
                             Instant createdAt, List<MemberResult> members, List<Map<String, Object>> items) {
    public static GameRoomResult from(GameRoom room) {
        List<Map<String, Object>> items = ((List<?>) room.configSnapshot().getOrDefault("items", List.of())).stream()
                .map(value -> (Map<String, Object>) value)
                .map(item -> {
                    var visible = new java.util.LinkedHashMap<>(item);
                    visible.remove("answerSpecJson");
                    visible.remove("explanation");
                    return java.util.Collections.unmodifiableMap(visible);
                }).toList();
        List<MemberResult> members = room.members().values().stream()
                .map(member -> new MemberResult(member.userId(), member.role().name(), member.status().name(), member.joinedAt()))
                .toList();
        return new GameRoomResult(room.id(), room.roomCode(), room.hostUserId(), room.gameType(), room.learningDomain(),
                room.mode(), room.maxPlayers(), room.status().name(), room.createdAt(), members, items);
    }
    public record MemberResult(UUID userId, String role, String status, Instant joinedAt) {}
}
