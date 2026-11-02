package com.ieltspath.game.infrastructure.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.game.application.result.GameRoomResult;
import com.ieltspath.game.application.result.MatchStartedEvent;
import com.ieltspath.game.application.result.RoomClosedEvent;
import com.ieltspath.game.application.result.RoomMemberKickedEvent;
import com.ieltspath.game.application.result.RoomUpdatedEvent;
import com.ieltspath.game.application.result.RocketGameplayResults;
import com.ieltspath.game.application.result.RocketRoundEvents;
import com.ieltspath.game.application.result.SoloGameplayEvent;
import com.ieltspath.game.application.result.WordlyMatchUpdatedEvent;
import com.ieltspath.game.application.result.WordlyPlayerLeftEvent;
import com.ieltspath.game.domain.vo.RocketEndReason;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoomEventCodecTest {
    private final RoomEventCodec codec = new RoomEventCodec(new ObjectMapper().findAndRegisterModules());
    private final UUID room = UUID.randomUUID();
    private final UUID match = UUID.randomUUID();
    private final UUID user = UUID.randomUUID();
    private final Instant at = Instant.parse("2026-10-31T12:00:00Z");

    @Test
    void everyRoomEventTypeSurvivesAWireRoundTripUnchanged() {
        List<Object> events = List.of(
                new RoomClosedEvent(room, "Phòng đã đóng"),
                new RoomUpdatedEvent(new GameRoomResult(room, "ABC123", user, "WORDLY", "VOCABULARY", "PRACTICE", 4,
                        "OPEN", at, List.of(), List.of())),
                new RoomMemberKickedEvent(room, user),
                new WordlyMatchUpdatedEvent(room, match, user, true, "Kết thúc"),
                new MatchStartedEvent(room, match),
                new SoloGameplayEvent(room, user, Map.of("type", "WORD_STATE", "guess", "AIR")),
                new WordlyPlayerLeftEvent(room, match, user, "Host"),
                new RocketRoundEvents.Started(room, match, UUID.randomUUID(), 1, 3, "Tài xế",
                        List.of(new RocketGameplayResults.Option(UUID.randomUUID(), "driver")), at, at.plusSeconds(15), at),
                new RocketRoundEvents.Answered(room, match, UUID.randomUUID(), user, 1, 2),
                new RocketRoundEvents.Closed(room, match, UUID.randomUUID(), UUID.randomUUID(),
                        List.of(new RocketRoundEvents.PlayerOutcome(user, UUID.randomUUID(), true, 180, 4, 180L, "ACTIVE")),
                        at.plusMillis(2500), false, RocketEndReason.values()[0]),
                new RocketRoundEvents.RankingChanged(room, match),
                new RocketRoundEvents.Finished(room, match, RocketEndReason.values()[0]));

        for (Object event : events) {
            String wire = codec.encode(event);
            Object decoded = codec.decode(wire);
            assertInstanceOf(event.getClass(), decoded);
            assertEquals(wire, codec.encode(decoded), event.getClass().getSimpleName());
        }
    }

    @Test
    void refusesToEncodeEventsOutsideTheRoomEventSet() {
        assertThrows(IllegalArgumentException.class, () -> codec.encode("not an event"));
    }

    @Test
    void refusesToDecodeAnUnknownOrUnsafeType() {
        String unknown = "{\"type\":\"java.lang.ProcessBuilder\",\"payload\":{}}";
        assertThrows(IllegalArgumentException.class, () -> codec.decode(unknown));
    }
}
