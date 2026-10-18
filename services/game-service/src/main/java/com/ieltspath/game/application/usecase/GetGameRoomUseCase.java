package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.result.GameRoomResult;
import com.ieltspath.game.domain.exception.GameRoomNotFoundException;
import com.ieltspath.game.domain.repository.GameRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetGameRoomUseCase {
    private final GameRoomRepository roomRepository;
    private final com.ieltspath.game.domain.repository.GameMatchRepository matches;

    public GetGameRoomUseCase(GameRoomRepository roomRepository, com.ieltspath.game.domain.repository.GameMatchRepository matches) { this.roomRepository = roomRepository; this.matches=matches; }

    @Transactional(readOnly = true)
    public GameRoomResult byId(UUID roomId, UUID userId) {
        var room = roomRepository.findById(roomId).orElseThrow(() -> new GameRoomNotFoundException(roomId));
        if (!room.hasActiveMember(userId)) throw new GameRoomNotFoundException(roomId);
        return GameRoomResult.from(room).withActiveMatchId(matches.findActiveIdByRoomId(room.id()).orElse(null));
    }

    @Transactional(readOnly = true)
    public GameRoomResult byCode(String roomCode, UUID userId) {
        var room = roomRepository.findByCode(roomCode).orElseThrow(() -> new GameRoomNotFoundException(roomCode));
        if (!room.hasActiveMember(userId)) throw new GameRoomNotFoundException(roomCode);
        return GameRoomResult.from(room).withActiveMatchId(matches.findActiveIdByRoomId(room.id()).orElse(null));
    }
}
