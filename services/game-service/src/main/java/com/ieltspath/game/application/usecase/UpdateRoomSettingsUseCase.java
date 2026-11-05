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

    public GameRoomResult execute(UUID roomId, UUID actorId, int initialLives, Integer wordLimit, Boolean timeEnabled) {
        return execute(roomId, actorId, initialLives, wordLimit, timeEnabled, null, null);
    }

    public GameRoomResult execute(UUID roomId, UUID actorId, int initialLives, Integer wordLimit, Boolean timeEnabled,
            Integer questionCount, Integer secondsPerQuestion) {
        return execute(roomId, actorId, initialLives, wordLimit, timeEnabled, questionCount, secondsPerQuestion, null);
    }

    /** Edits the settings of the room's current game; the Wordly or the Rocket fields apply, the others are ignored. */
    @Transactional
    public GameRoomResult execute(UUID roomId, UUID actorId, int initialLives, Integer wordLimit, Boolean timeEnabled,
            Integer questionCount, Integer secondsPerQuestion, com.ieltspath.game.domain.vo.WordFilter filter) {
        var room = rooms.findForUpdate(roomId).orElseThrow(() -> new GameRoomNotFoundException(roomId));
        if (room.settings() instanceof com.ieltspath.game.domain.vo.RocketRoomSettings)
            room.updateRocketSettings(actorId, initialLives, questionCount, secondsPerQuestion, filter, Instant.now());
        else room.updateSettings(actorId, initialLives, wordLimit, timeEnabled, filter, Instant.now());
        var result = GameRoomResult.from(rooms.save(room));
        events.publishEvent(new RoomUpdatedEvent(result));
        return result;
    }
}
