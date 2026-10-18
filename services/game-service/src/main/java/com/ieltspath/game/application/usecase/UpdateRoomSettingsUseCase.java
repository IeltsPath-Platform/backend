package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.result.GameRoomResult;
import com.ieltspath.game.application.result.RoomUpdatedEvent;
import com.ieltspath.game.domain.exception.GameRoomNotFoundException;
import com.ieltspath.game.domain.repository.GameRoomRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class UpdateRoomSettingsUseCase {
    private final GameRoomRepository rooms;
    private final ApplicationEventPublisher events;

    public UpdateRoomSettingsUseCase(GameRoomRepository rooms, ApplicationEventPublisher events) {
        this.rooms = rooms;
        this.events = events;
    }

    @Transactional
    public GameRoomResult execute(UUID roomId, UUID actorId, int initialLives, Integer wordLimit, Boolean timeEnabled) {
        var room = rooms.findForUpdate(roomId).orElseThrow(() -> new GameRoomNotFoundException(roomId));
        room.updateSettings(actorId, initialLives, wordLimit, timeEnabled, Instant.now());
        var result = GameRoomResult.from(rooms.save(room));
        events.publishEvent(new RoomUpdatedEvent(result));
        return result;
    }
}
