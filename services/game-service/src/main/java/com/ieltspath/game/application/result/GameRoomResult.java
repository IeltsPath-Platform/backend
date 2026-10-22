package com.ieltspath.game.application.result;

import com.ieltspath.game.domain.aggregate.GameRoom;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GameRoomResult(UUID id, String roomCode, UUID hostUserId, String gameType,
                             String learningDomain, String mode, int maxPlayers, String status,
                             Instant createdAt, List<MemberResult> members, List<GameItemResult> items, String playerMode, SettingsResult settings, UUID activeMatchId) {
    public GameRoomResult(UUID id, String roomCode, UUID hostUserId, String gameType, String learningDomain, String mode, int maxPlayers, String status, Instant createdAt, List<MemberResult> members, List<GameItemResult> items, String playerMode, SettingsResult settings) {
        this(id,roomCode,hostUserId,gameType,learningDomain,mode,maxPlayers,status,createdAt,members,items,playerMode,settings,null);
    }
    public GameRoomResult withActiveMatchId(UUID matchId) {
        return new GameRoomResult(id,roomCode,hostUserId,gameType,learningDomain,mode,maxPlayers,status,createdAt,members,items,playerMode,settings,matchId);
    }
    public GameRoomResult(UUID id, String roomCode, UUID hostUserId, String gameType, String learningDomain, String mode, int maxPlayers, String status, Instant createdAt, List<MemberResult> members, List<GameItemResult> items) {
        this(id, roomCode, hostUserId, gameType, learningDomain, mode, maxPlayers, status, createdAt, members, items, "MULTIPLAYER", null);
    }
    public static GameRoomResult from(GameRoom room) {
        List<GameItemResult> items = ((List<?>) room.configSnapshot().getOrDefault("items", List.of())).stream()
                .map(value -> (Map<String, Object>) value)
                .map(GameItemResult::from).toList();
        List<MemberResult> members = room.members().values().stream()
                .sorted(java.util.Comparator.comparing(GameRoom.Member::slotNumber,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                        .thenComparing(GameRoom.Member::joinedAt).thenComparing(GameRoom.Member::id))
                .map(member -> new MemberResult(member.userId(), member.role().name(), member.status().name(), member.joinedAt(), member.slotNumber(), member.displayName()))
                .toList();
        return new GameRoomResult(room.id(), room.roomCode(), room.hostUserId(), room.gameType(), room.learningDomain(),
                room.mode(), room.maxPlayers(), room.status().name(), room.createdAt(), members, items, room.playerMode(),
                SettingsResult.of(room.settings()));
    }
    /**
     * Wordly fills the first five fields; Rocket fills initialLives, questionCount and secondsPerQuestion, with
     * timeEnabled always true and the Wordly-only fields null.
     */
    public record SettingsResult(int initialLives, Integer wordTimeLimitSeconds, String playMode, Integer wordLimit,
            boolean timeEnabled, Integer questionCount, Integer secondsPerQuestion) {
        public SettingsResult(int initialLives, Integer wordTimeLimitSeconds, String playMode, Integer wordLimit, boolean timeEnabled) {
            this(initialLives, wordTimeLimitSeconds, playMode, wordLimit, timeEnabled, null, null);
        }

        static SettingsResult of(com.ieltspath.game.domain.vo.RoomSettings settings) {
            return switch (settings) {
                case com.ieltspath.game.domain.vo.WordlyRoomSettings w -> new SettingsResult(w.initialLives(),
                        w.wordTimeLimitSeconds(), w.playMode().name(), w.wordLimit(), w.timeEnabled());
                case com.ieltspath.game.domain.vo.RocketRoomSettings r -> new SettingsResult(r.initialLives(), null, null,
                        null, true, r.questionCount(), r.secondsPerQuestion());
                case null, default -> null;
            };
        }
    }
    public record MemberResult(UUID userId, String role, String status, Instant joinedAt, Integer slotNumber, String displayName) {
        public MemberResult(UUID userId, String role, String status, Instant joinedAt) { this(userId, role, status, joinedAt, null, null); }
    }
}
