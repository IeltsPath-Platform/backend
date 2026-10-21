package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.command.CreateWordlyRoomCommand;
import com.ieltspath.game.application.port.OutboxWriter;
import com.ieltspath.game.application.result.GameRoomResult;
import com.ieltspath.game.domain.aggregate.GameRoom;
import com.ieltspath.game.domain.repository.GameRoomRepository;
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
        admission.requireNoActiveGame(command.hostUserId());
        String code = availableCode();
        var room = GameRoom.wordly(UUID.randomUUID(), code, command.hostUserId(), command.settings(), Instant.now());
        room.setMemberDisplayName(command.hostUserId(), profile.displayName());
        var saved = rooms.save(room);
        outbox.append("GameRoom", room.id().toString(), "GameRoomCreated", Map.of(
                "roomId", room.id().toString(), "hostUserId", room.hostUserId().toString(),
                "roomCode", code, "playerMode", room.playerMode()));
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
