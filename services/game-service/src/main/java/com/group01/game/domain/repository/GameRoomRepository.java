package com.group01.game.domain.repository;

import com.group01.game.domain.aggregate.GameRoom;

import java.util.Optional;
import java.util.UUID;

public interface GameRoomRepository {
    GameRoom save(GameRoom room);
    Optional<GameRoom> findById(UUID id);
    Optional<GameRoom> findForUpdate(UUID id);
    Optional<GameRoom> findByCode(String roomCode);
}
