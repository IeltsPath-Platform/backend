package com.group01.game.application.usecase;

import com.group01.game.application.result.GameRoomResult;
import com.group01.game.domain.exception.GameRoomNotFoundException;
import com.group01.game.domain.repository.GameRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetGameRoomUseCase {
    private final GameRoomRepository roomRepository;

    public GetGameRoomUseCase(GameRoomRepository roomRepository) { this.roomRepository = roomRepository; }

    @Transactional(readOnly = true)
    public GameRoomResult byId(UUID roomId, UUID userId) {
        var room = roomRepository.findById(roomId).orElseThrow(() -> new GameRoomNotFoundException(roomId));
        if (!room.hasActiveMember(userId)) throw new GameRoomNotFoundException(roomId);
        return GameRoomResult.from(room);
    }

    @Transactional(readOnly = true)
    public GameRoomResult byCode(String roomCode, UUID userId) {
        var room = roomRepository.findByCode(roomCode).orElseThrow(() -> new GameRoomNotFoundException(roomCode));
        if (!room.hasActiveMember(userId)) throw new GameRoomNotFoundException(roomCode);
        return GameRoomResult.from(room);
    }
}
