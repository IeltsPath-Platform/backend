package com.ieltspath.game.domain.aggregate;

import com.ieltspath.game.domain.exception.InvalidGameRoomStateException;
import com.ieltspath.game.domain.vo.RoomGame;
import com.ieltspath.game.domain.vo.RoomSettings;
import com.ieltspath.game.domain.vo.WordlyRoomSettings;

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
    private RoomSettings settings;

    public static GameRoom wordly(UUID id, String code, UUID host, WordlyRoomSettings settings, Instant now) {
        return open(id, code, host, settings, now);
    }

    /** A room for one of the {@link RoomGame}s; the settings decide the game, player mode and capacity. */
    public static GameRoom open(UUID id, String code, UUID host, RoomSettings settings, Instant now) {
        if (code == null || !code.matches("[A-Z0-9]{6}")) throw new IllegalArgumentException("Room code must have 6 characters");
        java.util.Objects.requireNonNull(host, "Host is required");
        java.util.Objects.requireNonNull(settings, "Settings are required");
        GameRoom room = new GameRoom(id, code, host, settings.maxPlayers(), settings.game().name(), "VOCABULARY",
                "PRACTICE", Map.of(), now, null, UUID.randomUUID());
        room.settings = settings;
        Member member = room.members.get(host);
        room.members.put(host, new Member(member.id(), host, MemberRole.HOST, MemberStatus.JOINED, now, 1));
        return room;
    }

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
                                        Instant expiresAt, GameRoomStatus status, java.util.List<Member> members,
                                        RoomSettings settings) {
        if (settings != null && !settings.game().name().equals(gameType))
            throw new IllegalArgumentException("Settings do not belong to " + gameType);
        UUID hostMemberId = members.stream().filter(member -> member.role() == MemberRole.HOST)
                .map(Member::id).findFirst().orElseThrow();
        GameRoom room = new GameRoom(id, roomCode, hostUserId, maxPlayers, gameType, learningDomain, mode,
                configSnapshot, createdAt, expiresAt, hostMemberId);
        room.members.clear();
        members.forEach(member -> room.members.put(member.userId(), member));
        room.status = status;
        room.updatedAt = updatedAt;
        room.settings = settings;
        return room;
    }

    public void join(UUID userId, Instant joinedAt) {
        join(UUID.randomUUID(), userId, joinedAt);
    }

    public void join(UUID memberId, UUID userId, Instant joinedAt) {
        requireWaiting();
        if ("SOLO".equals(playerMode())) throw new InvalidGameRoomStateException("Solo rooms cannot be joined");
        Member existing = members.get(userId);
        if (existing != null && existing.status() != MemberStatus.LEFT) {
            throw new InvalidGameRoomStateException("User is already a member of this room");
        }
        long activeMembers = members.values().stream().filter(Member::active).count();
        if (activeMembers >= maxPlayers) throw new InvalidGameRoomStateException("Room is full");
        members.put(userId, new Member(existing == null ? memberId : existing.id(), userId,
                MemberRole.PLAYER, MemberStatus.JOINED, joinedAt, nextSlot()));
        updatedAt = joinedAt;
    }

    public void setReady(UUID userId, boolean ready, Instant at) {
        requireWaiting();
        Member member = members.get(userId);
        if (member == null || !member.active()) throw new InvalidGameRoomStateException("User is not an active room member");
        members.put(userId, member.withStatus(ready ? MemberStatus.READY : MemberStatus.JOINED));
        updatedAt = at;
    }

    public void updateSettings(UUID actorId, int initialLives, Integer wordLimit, Instant at) {
        updateSettings(actorId, initialLives, wordLimit, null, at);
    }

    public void updateSettings(UUID actorId, int initialLives, Integer wordLimit, Boolean timeEnabled, Instant at) {
        if (!hostUserId.equals(actorId)) throw new com.ieltspath.game.domain.exception.GameRoomForbiddenException("Only the host can edit room settings");
        requireWaiting();
        if (!(settings instanceof WordlyRoomSettings wordly)) throw new InvalidGameRoomStateException("Room is not a Wordly room");
        WordlyRoomSettings next = new WordlyRoomSettings(wordly.playerMode(), initialLives,
                wordly.playMode(), wordLimit, timeEnabled == null ? wordly.timeEnabled() : timeEnabled);
        if (next.equals(wordly)) return;
        replaceSettings(next, at);
    }

    /** Host-only, while waiting. A null question count or time keeps the current value. */
    public void updateRocketSettings(UUID actorId, int initialLives, Integer questionCount, Integer secondsPerQuestion, Instant at) {
        if (!hostUserId.equals(actorId)) throw new com.ieltspath.game.domain.exception.GameRoomForbiddenException("Only the host can edit room settings");
        requireWaiting();
        if (!(settings instanceof com.ieltspath.game.domain.vo.RocketRoomSettings rocket))
            throw new InvalidGameRoomStateException("Room is not a Rocket room");
        var next = new com.ieltspath.game.domain.vo.RocketRoomSettings(rocket.solo(),
                questionCount == null ? rocket.questionCount() : questionCount, initialLives,
                secondsPerQuestion == null ? rocket.secondsPerQuestion() : secondsPerQuestion);
        if (next.equals(rocket)) return;
        replaceSettings(next, at);
    }

    /** New settings un-ready everyone, so nobody starts on terms they did not see. */
    private void replaceSettings(RoomSettings next, Instant at) {
        settings = next;
        members.replaceAll((id, member) -> member.status() == MemberStatus.READY
                ? member.withStatus(MemberStatus.JOINED) : member);
        updatedAt = at;
    }

    public void close(UUID actorId, Instant at) {
        requireHost(actorId);
        requireWaiting();
        status = GameRoomStatus.CLOSED;
        updatedAt = at;
    }

    public boolean canStart() {
        long count = members.values().stream().filter(Member::active).count();
        return status == GameRoomStatus.WAITING && count >= ("SOLO".equals(playerMode()) ? 1 : 2)
                && members.values().stream().filter(Member::active).allMatch(m -> m.status() == MemberStatus.READY);
    }

    public void startMatch(UUID actorId, Instant at) {
        requireHost(actorId);
        requireWaiting();

        if (members.values().stream().filter(Member::active).count() < ("SOLO".equals(playerMode()) ? 1 : 2)) {
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
        if (hostUserId.equals(userId)) status = GameRoomStatus.CLOSED;
    }

    public void kick(UUID actorId, UUID userId, Instant at) {
        if (!hostUserId.equals(actorId)) throw new com.ieltspath.game.domain.exception.GameRoomForbiddenException("Only the host can kick room members");
        requireWaiting();
        if (hostUserId.equals(userId)) throw new InvalidGameRoomStateException("The host cannot kick themselves");
        Member member = members.get(userId);
        if (member == null || !member.active()) throw new InvalidGameRoomStateException("User is not an active room member");
        members.put(userId, member.withStatus(MemberStatus.LEFT));
        updatedAt = at;
    }

    public void setMemberDisplayName(UUID userId, String name) {
        if (name == null || name.isBlank() || name.length() > 255) throw new IllegalArgumentException("Valid display name is required");
        Member member = members.get(userId);
        if (member == null) throw new InvalidGameRoomStateException("User is not a room member");
        members.put(userId, new Member(member.id(), userId, member.role(), member.status(), member.joinedAt(), member.slotNumber(), name.trim()));
    }

    private void requireWaiting() {
        if (status != GameRoomStatus.WAITING) throw new InvalidGameRoomStateException("Room is not waiting for players");
    }

    private Integer nextSlot() {
        if (settings == null) return null;
        for (int slot = 1; slot <= maxPlayers; slot++) {
            final int candidate = slot;
            if (members.values().stream().noneMatch(m -> m.active() && Integer.valueOf(candidate).equals(m.slotNumber()))) return slot;
        }
        throw new InvalidGameRoomStateException("Room is full");
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
    public String playerMode() { return settings != null && settings.solo() ? "SOLO" : "MULTIPLAYER"; }
    /** Null for legacy session rooms, which predate room settings. */
    public RoomSettings settings() { return settings; }
    public java.util.Optional<RoomGame> roomGame() { return RoomGame.of(gameType); }
    public WordlyRoomSettings wordlySettings() { return settings instanceof WordlyRoomSettings wordly ? wordly : null; }
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

    public record Member(UUID id, UUID userId, MemberRole role, MemberStatus status, Instant joinedAt, Integer slotNumber, String displayName) {
        public Member(UUID id, UUID userId, MemberRole role, MemberStatus status, Instant joinedAt, Integer slotNumber) { this(id, userId, role, status, joinedAt, slotNumber, null); }
        public Member(UUID id, UUID userId, MemberRole role, MemberStatus status, Instant joinedAt) {
            this(id, userId, role, status, joinedAt, null, null);
        }
        public boolean active() { return status == MemberStatus.JOINED || status == MemberStatus.READY || status == MemberStatus.DISCONNECTED; }
        Member withStatus(MemberStatus value) { return new Member(id, userId, role, value, joinedAt, slotNumber, displayName); }
    }
}
