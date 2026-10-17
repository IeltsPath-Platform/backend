package com.ieltspath.game.infrastructure.websocket;

import com.ieltspath.game.application.result.GameItemResult;
import com.ieltspath.game.application.result.GameRoomResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoomStateMessage(String type, Room room) {
    public static RoomStateMessage of(String type, GameRoomResult result) {
        List<Member> members = result.members().stream()
                .map(member -> new Member(member.userId(), member.role(), member.status(), member.joinedAt(), member.displayName(), member.slotNumber())).toList();
        List<Item> items = result.items().stream().map(Item::from).toList();
        return new RoomStateMessage(type, new Room(result.id(), result.roomCode(), result.hostUserId(), result.gameType(),
                result.learningDomain(), result.mode(), result.maxPlayers(), result.status(), result.createdAt(),
                members, items, result.playerMode(), result.settings(),result.activeMatchId()));
    }

    public record Room(UUID id, String roomCode, UUID hostUserId, String gameType, String learningDomain,
                       String mode, int maxPlayers, String status, Instant createdAt,
                       List<Member> members, List<Item> items, String playerMode, GameRoomResult.SettingsResult settings,UUID activeMatchId) {
        @com.fasterxml.jackson.annotation.JsonProperty("canStart")
        public boolean canStart() {
            var active = members.stream().filter(m -> !"LEFT".equals(m.status())).toList();
            return "WAITING".equals(status) && active.size() >= ("SOLO".equals(playerMode) ? 1 : 2)
                    && active.stream().allMatch(m -> "READY".equals(m.status()));
        }
    }

    public record Member(UUID userId, String role, String status, Instant joinedAt, String displayName, Integer slotNumber) {
    }

    public record Item(Object canonicalId, Object vocabularySenseId, Object questionVersionId,
                       Object prompt, Object options) {
        static Item from(GameItemResult item) {
            return new Item(item.canonicalId(), item.vocabularySenseId(), item.questionVersionId(),
                    item.prompt(), item.options());
        }
    }
}
