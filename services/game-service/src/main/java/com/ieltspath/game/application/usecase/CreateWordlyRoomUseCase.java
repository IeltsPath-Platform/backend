package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.command.CreateWordlyRoomCommand;
import com.ieltspath.game.application.port.OutboxWriter;
import com.ieltspath.game.application.result.GameRoomResult;
import com.ieltspath.game.domain.aggregate.GameRoom;
import com.ieltspath.game.domain.repository.GameRoomRepository;
import com.ieltspath.game.domain.vo.RoomSettings;
import org.springframework.stereotype.Service;
import com.ieltspath.game.application.port.CurrentPlayerProfile;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class CreateWordlyRoomUseCase {
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();
    private final GameRoomRepository rooms;
    private final CurrentPlayerProfile profile;
    private final OutboxWriter outbox;
    private final WordlyRoomAdmission admission;

    public CreateWordlyRoomUseCase(GameRoomRepository rooms, OutboxWriter outbox, CurrentPlayerProfile profile,
                                   WordlyRoomAdmission admission) {
        this.admission = admission;
        this.profile = profile;
        this.rooms = rooms;
        this.outbox = outbox;
    }

    @Transactional
    public GameRoomResult execute(CreateWordlyRoomCommand command) {
        return open(command.hostUserId(), command.settings());
    }

    /** Creates a room for any room game; the settings decide which. The name predates Rocket. */
    @Transactional
    public GameRoomResult open(UUID hostUserId, RoomSettings settings) {
        admission.requireNoActiveGame(hostUserId);
        String code = availableCode();
        var room = GameRoom.open(UUID.randomUUID(), code, hostUserId, settings, Instant.now());
        room.setMemberDisplayName(hostUserId, profile.displayName());
        var saved = rooms.save(room);
        outbox.append("GameRoom", room.id().toString(), "GameRoomCreated", Map.of(
                "roomId", room.id().toString(), "hostUserId", room.hostUserId().toString(),
                "roomCode", code, "playerMode", room.playerMode(), "gameType", room.gameType()));
        return GameRoomResult.from(saved);
    }

    private String availableCode() {
        for (int attempt = 0; attempt < 8; attempt++) {
            StringBuilder code = new StringBuilder(6);
            for (int i = 0; i < 6; i++) code.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            if (rooms.findByCode(code.toString()).isEmpty()) return code.toString();
        }
        throw new IllegalStateException("Unable to allocate a room code; retry the request");
    }
}
