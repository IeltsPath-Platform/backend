package com.group01.game.domain.aggregate;

import com.group01.game.domain.exception.InvalidGameRoomStateException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class GameRoom {
    private final UUID id;
    private final String roomCode;
    private final UUID hostUserId;
    private final int maxPlayers;
    private final String gameType;
    private final String learningDomain;
    private final String mode;
    private final Map<String, Object> configSnapshot;
    private final Instant createdAt;
    private final Instant expiresAt;
    private Instant updatedAt;
    private final Map<UUID, Member> members = new LinkedHashMap<>();
    private GameRoomStatus status = GameRoomStatus.WAITING;

    public GameRoom(UUID id, String roomCode, UUID hostUserId, int maxPlayers, Instant createdAt) {
        this(id, roomCode, hostUserId, maxPlayers, "WORD_MEANING_MATCH", "VOCABULARY", "PRACTICE",
                Map.of(), createdAt, null, UUID.randomUUID());
    }

    public GameRoom(UUID id, String roomCode, UUID hostUserId, int maxPlayers, String gameType,
                    String learningDomain, String mode, Map<String, Object> configSnapshot,
                    Instant createdAt, Instant expiresAt, UUID hostMemberId) {
        if (maxPlayers < 1) throw new IllegalArgumentException("maxPlayers must be positive");
        this.id = id;
        this.roomCode = roomCode;
        this.hostUserId = hostUserId;
        this.maxPlayers = maxPlayers;
        this.gameType = gameType;
        this.learningDomain = learningDomain;
        this.mode = mode;
        this.configSnapshot = Map.copyOf(configSnapshot);
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.expiresAt = expiresAt;
        this.members.put(hostUserId, new Member(hostMemberId, hostUserId, MemberRole.HOST, MemberStatus.JOINED, createdAt));
    }

    public static GameRoom reconstitute(UUID id, String roomCode, UUID hostUserId, int maxPlayers,
                                        String gameType, String learningDomain, String mode,
                                        Map<String, Object> configSnapshot, Instant createdAt, Instant updatedAt,
                                        Instant expiresAt, GameRoomStatus status, java.util.List<Member> members) {
        UUID hostMemberId = members.stream().filter(member -> member.role() == MemberRole.HOST)
                .map(Member::id).findFirst().orElseThrow();
        GameRoom room = new GameRoom(id, roomCode, hostUserId, maxPlayers, gameType, learningDomain, mode,
                configSnapshot, createdAt, expiresAt, hostMemberId);
        room.members.clear();
        members.forEach(member -> room.members.put(member.userId(), member));
        room.status = status;
        room.updatedAt = updatedAt;
        return room;
    }

    public void join(UUID userId, Instant joinedAt) {
        join(UUID.randomUUID(), userId, joinedAt);
    }

    public void join(UUID memberId, UUID userId, Instant joinedAt) {
        requireWaiting();
        Member existing = members.get(userId);
        if (existing != null && existing.status() != MemberStatus.LEFT) {
            throw new InvalidGameRoomStateException("User is already a member of this room");
        }
        long activeMembers = members.values().stream().filter(Member::active).count();
        if (activeMembers >= maxPlayers) throw new InvalidGameRoomStateException("Room is full");
        members.put(userId, new Member(existing == null ? memberId : existing.id(), userId,
                MemberRole.PLAYER, MemberStatus.JOINED, joinedAt));
        updatedAt = joinedAt;
    }

    public void setReady(UUID userId, boolean ready, Instant at) {
        requireWaiting();
        Member member = members.get(userId);
        if (member == null || !member.active()) throw new InvalidGameRoomStateException("User is not an active room member");
        members.put(userId, member.withStatus(ready ? MemberStatus.READY : MemberStatus.JOINED));
        updatedAt = at;
    }

    public void close(UUID actorId, Instant at) {
        requireHost(actorId);
        requireWaiting();
        status = GameRoomStatus.CLOSED;
        updatedAt = at;
    }

    public void startMatch(UUID actorId, Instant at) {
        requireHost(actorId);
        requireWaiting();
        if (members.values().stream().filter(Member::active).count() < 2) {
            throw new InvalidGameRoomStateException("At least two active members are required to start");
        }
        if (members.values().stream().filter(Member::active).anyMatch(m -> m.status() != MemberStatus.READY)) {
            throw new InvalidGameRoomStateException("Every active member must be ready before starting");
        }
        status = GameRoomStatus.IN_MATCH;
        updatedAt = at;
    }

    public void leave(UUID userId, Instant leftAt) {
        requireWaiting();
        Member member = members.get(userId);
        if (member == null || !member.active()) throw new InvalidGameRoomStateException("User is not an active room member");
        members.put(userId, member.withStatus(MemberStatus.LEFT));
        updatedAt = leftAt;
    }

    private void requireWaiting() {
        if (status != GameRoomStatus.WAITING) throw new InvalidGameRoomStateException("Room is not waiting for players");
    }

    private void requireHost(UUID actorId) {
        if (!hostUserId.equals(actorId)) throw new InvalidGameRoomStateException("Only the host can perform this action");
    }

    public UUID id() { return id; }
    public String roomCode() { return roomCode; }
    public UUID hostUserId() { return hostUserId; }
    public int maxPlayers() { return maxPlayers; }
    public String gameType() { return gameType; }
    public String learningDomain() { return learningDomain; }
    public String mode() { return mode; }
    public Map<String, Object> configSnapshot() { return configSnapshot; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public Instant expiresAt() { return expiresAt; }
    public Map<UUID, Member> members() { return Map.copyOf(members); }
    public boolean hasActiveMember(UUID userId) {
        Member member = members.get(userId);
        return member != null && member.active();
    }
    public GameRoomStatus status() { return status; }

    public enum MemberRole { HOST, PLAYER }
    public enum MemberStatus { JOINED, READY, LEFT, DISCONNECTED }
    public enum GameRoomStatus { WAITING, IN_MATCH, CLOSED, EXPIRED }

    public record Member(UUID id, UUID userId, MemberRole role, MemberStatus status, Instant joinedAt) {
        public boolean active() { return status == MemberStatus.JOINED || status == MemberStatus.READY || status == MemberStatus.DISCONNECTED; }
        Member withStatus(MemberStatus value) { return new Member(id, userId, role, value, joinedAt); }
    }
}
