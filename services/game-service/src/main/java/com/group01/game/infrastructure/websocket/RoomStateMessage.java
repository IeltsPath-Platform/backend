package com.group01.game.infrastructure.websocket;

import com.group01.game.application.result.GameItemResult;
import com.group01.game.application.result.GameRoomResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoomStateMessage(String type, Room room) {
    public static RoomStateMessage of(String type, GameRoomResult result) {
        List<Member> members = result.members().stream()
                .map(member -> new Member(member.userId(), member.role(), member.status(), member.joinedAt())).toList();
        List<Item> items = result.items().stream().map(Item::from).toList();
        return new RoomStateMessage(type, new Room(result.id(), result.roomCode(), result.hostUserId(), result.gameType(),
                result.learningDomain(), result.mode(), result.maxPlayers(), result.status(), result.createdAt(),
                members, items));
    }

    public record Room(UUID id, String roomCode, UUID hostUserId, String gameType, String learningDomain,
                       String mode, int maxPlayers, String status, Instant createdAt,
                       List<Member> members, List<Item> items) {
    }

    public record Member(UUID userId, String role, String status, Instant joinedAt) {
    }

    public record Item(Object canonicalId, Object vocabularySenseId, Object questionVersionId,
                       Object prompt, Object options) {
        static Item from(GameItemResult item) {
            return new Item(item.canonicalId(), item.vocabularySenseId(), item.questionVersionId(),
                    item.prompt(), item.options());
        }
    }
}
