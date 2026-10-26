package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.port.OutboxWriter;
import com.ieltspath.game.application.result.GameRoomResult;
import com.ieltspath.game.domain.exception.GameRoomNotFoundException;
import com.ieltspath.game.domain.repository.GameRoomRepository;
import com.ieltspath.game.domain.vo.RoomGame;
import org.springframework.stereotype.Service;
import com.ieltspath.game.application.port.CurrentPlayerProfile;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class ChangeGameRoomMembershipUseCase {
    private final GameRoomRepository roomRepository;
    private final org.springframework.context.ApplicationEventPublisher events;
    private final CurrentPlayerProfile profile;
    private final OutboxWriter outboxWriter;
    private final WordlyRoomAdmission admission;

    public ChangeGameRoomMembershipUseCase(GameRoomRepository roomRepository, OutboxWriter outboxWriter, CurrentPlayerProfile profile, org.springframework.context.ApplicationEventPublisher events, WordlyRoomAdmission admission) {
        this.admission = admission;
        this.events = events;
        this.profile = profile;
        this.roomRepository = roomRepository;
        this.outboxWriter = outboxWriter;
    }

    @Transactional
    public GameRoomResult join(UUID roomId, UUID userId) {
        // Take the user admission lock before the room lock, matching room creation.
        if (roomRepository.findById(roomId).filter(found -> RoomGame.isRoomGame(found.gameType())).isPresent())
            admission.requireNoActiveGame(userId);
        var room = roomRepository.findForUpdate(roomId).orElseThrow(() -> new GameRoomNotFoundException(roomId));
        room.join(UUID.randomUUID(), userId, Instant.now());
        room.setMemberDisplayName(userId, profile.displayName());
        var saved = roomRepository.save(room);
        outboxWriter.append("GameRoom", roomId.toString(), "GameRoomMemberJoined",
                Map.of("roomId", roomId.toString(), "userId", userId.toString()));
        var result = GameRoomResult.from(saved);
        if ("WAITING".equals(result.status())) events.publishEvent(new com.ieltspath.game.application.result.RoomUpdatedEvent(result));
        return result;
    }

    @Transactional
    public GameRoomResult joinByCode(String roomCode, UUID userId) {
        var room = roomRepository.findByCode(roomCode).orElseThrow(() -> new GameRoomNotFoundException(roomCode));
        return join(room.id(), userId);
    }

    @Transactional
    public GameRoomResult ready(UUID roomId, UUID userId, boolean ready) {
        var room = roomRepository.findForUpdate(roomId).orElseThrow(() -> new GameRoomNotFoundException(roomId));
        room.setReady(userId, ready, Instant.now());
        var saved = roomRepository.save(room);
        outboxWriter.append("GameRoom", roomId.toString(), ready ? "GameRoomMemberReady" : "GameRoomMemberNotReady",
                Map.of("roomId", roomId.toString(), "userId", userId.toString()));
        var result = GameRoomResult.from(saved);
        if ("WAITING".equals(result.status())) events.publishEvent(new com.ieltspath.game.application.result.RoomUpdatedEvent(result));
        return result;
    }

    @Transactional
    public GameRoomResult leave(UUID roomId, UUID userId) {
        var room = roomRepository.findForUpdate(roomId).orElseThrow(() -> new GameRoomNotFoundException(roomId));
        room.leave(userId, Instant.now());
        publishClosure(room);
        var saved = roomRepository.save(room);
        outboxWriter.append("GameRoom", roomId.toString(), "GameRoomMemberLeft",
                Map.of("roomId", roomId.toString(), "userId", userId.toString()));
        var result = GameRoomResult.from(saved);
        if ("WAITING".equals(result.status())) events.publishEvent(new com.ieltspath.game.application.result.RoomUpdatedEvent(result));
        return result;
    }

    @Transactional
    public GameRoomResult kick(UUID roomId, UUID actorId, UUID userId) {
        var room = roomRepository.findForUpdate(roomId).orElseThrow(() -> new GameRoomNotFoundException(roomId));
        room.kick(actorId, userId, Instant.now());
        var saved = roomRepository.save(room);
        outboxWriter.append("GameRoom", roomId.toString(), "GameRoomMemberKicked",
                Map.of("roomId", roomId.toString(), "userId", userId.toString()));
        var result = GameRoomResult.from(saved);
        events.publishEvent(new com.ieltspath.game.application.result.RoomMemberKickedEvent(roomId, userId));
        events.publishEvent(new com.ieltspath.game.application.result.RoomUpdatedEvent(result));
        return result;
    }

    private void publishClosure(com.ieltspath.game.domain.aggregate.GameRoom room) {
        if (room.status() != com.ieltspath.game.domain.aggregate.GameRoom.GameRoomStatus.CLOSED) return;
        String name = room.members().get(room.hostUserId()).displayName();
        events.publishEvent(new com.ieltspath.game.application.result.RoomClosedEvent(room.id(),
                "Chủ phòng " + (name == null ? room.hostUserId().toString() : name) + " đã đóng phòng"));
    }

    @Transactional
    public GameRoomResult close(UUID roomId, UUID userId) {
        var room = roomRepository.findForUpdate(roomId).orElseThrow(() -> new GameRoomNotFoundException(roomId));
        room.close(userId, Instant.now());
        publishClosure(room);
        var saved = roomRepository.save(room);
        outboxWriter.append("GameRoom", roomId.toString(), "GameRoomClosed",
                Map.of("roomId", roomId.toString(), "userId", userId.toString()));
        var result = GameRoomResult.from(saved);
        if ("WAITING".equals(result.status())) events.publishEvent(new com.ieltspath.game.application.result.RoomUpdatedEvent(result));
        return result;
    }
}
