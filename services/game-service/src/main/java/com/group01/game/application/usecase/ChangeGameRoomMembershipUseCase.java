package com.group01.game.application.usecase;

import com.group01.game.application.port.OutboxWriter;
import com.group01.game.application.result.GameRoomResult;
import com.group01.game.domain.exception.GameRoomNotFoundException;
import com.group01.game.domain.repository.GameRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class ChangeGameRoomMembershipUseCase {
    private final GameRoomRepository roomRepository;
    private final OutboxWriter outboxWriter;

    public ChangeGameRoomMembershipUseCase(GameRoomRepository roomRepository, OutboxWriter outboxWriter) {
        this.roomRepository = roomRepository;
        this.outboxWriter = outboxWriter;
    }

    @Transactional
    public GameRoomResult join(UUID roomId, UUID userId) {
        var room = roomRepository.findForUpdate(roomId).orElseThrow(() -> new GameRoomNotFoundException(roomId));
        room.join(UUID.randomUUID(), userId, Instant.now());
        var saved = roomRepository.save(room);
        outboxWriter.append("GameRoom", roomId.toString(), "GameRoomMemberJoined",
                Map.of("roomId", roomId.toString(), "userId", userId.toString()));
        return GameRoomResult.from(saved);
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
        return GameRoomResult.from(saved);
    }

    @Transactional
    public GameRoomResult leave(UUID roomId, UUID userId) {
        var room = roomRepository.findForUpdate(roomId).orElseThrow(() -> new GameRoomNotFoundException(roomId));
        room.leave(userId, Instant.now());
        var saved = roomRepository.save(room);
        outboxWriter.append("GameRoom", roomId.toString(), "GameRoomMemberLeft",
                Map.of("roomId", roomId.toString(), "userId", userId.toString()));
        return GameRoomResult.from(saved);
    }

    @Transactional
    public GameRoomResult close(UUID roomId, UUID userId) {
        var room = roomRepository.findForUpdate(roomId).orElseThrow(() -> new GameRoomNotFoundException(roomId));
        room.close(userId, Instant.now());
        var saved = roomRepository.save(room);
        outboxWriter.append("GameRoom", roomId.toString(), "GameRoomClosed",
                Map.of("roomId", roomId.toString(), "userId", userId.toString()));
        return GameRoomResult.from(saved);
    }
}
