package com.ieltspath.game.infrastructure.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ieltspath.game.application.result.GameRoomResult;
import com.ieltspath.game.application.result.MatchStartedEvent;
import com.ieltspath.game.application.result.RoomClosedEvent;
import com.ieltspath.game.application.result.RoomMemberKickedEvent;
import com.ieltspath.game.application.result.RoomUpdatedEvent;
import com.ieltspath.game.application.result.RocketRoundEvents;
import com.ieltspath.game.application.result.SoloGameplayEvent;
import com.ieltspath.game.application.result.WordlyMatchUpdatedEvent;
import com.ieltspath.game.application.result.WordlyPlayerLeftEvent;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Turns the room events that the WebSocket handler reacts to into a JSON envelope for the Redis channel, and back.
 * Only the listed event types are accepted in either direction, so a message can never name an arbitrary class.
 */
@Component
public class RoomEventCodec {
    private static final Set<Class<?>> EVENT_TYPES = Set.of(
            RoomClosedEvent.class, RoomUpdatedEvent.class, RoomMemberKickedEvent.class,
            WordlyMatchUpdatedEvent.class, MatchStartedEvent.class, SoloGameplayEvent.class,
            WordlyPlayerLeftEvent.class,
            RocketRoundEvents.Started.class, RocketRoundEvents.Answered.class, RocketRoundEvents.Closed.class,
            RocketRoundEvents.RankingChanged.class, RocketRoundEvents.Finished.class);

    private static final Map<String, Class<?>> BY_NAME = EVENT_TYPES.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(Class::getName, type -> type));

    private final ObjectMapper mapper;

    public RoomEventCodec(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public boolean isRoomEvent(Object event) {
        return event != null && EVENT_TYPES.contains(event.getClass());
    }

    public String encode(Object event) {
        if (!isRoomEvent(event)) {
            throw new IllegalArgumentException("Not a room event: " + (event == null ? "null" : event.getClass().getName()));
        }
        ObjectNode envelope = mapper.createObjectNode();
        envelope.put("type", event.getClass().getName());
        envelope.set("payload", mapper.valueToTree(event));
        return envelope.toString();
    }

    public Object decode(String wire) {
        try {
            JsonNode envelope = mapper.readTree(wire);
            Class<?> type = BY_NAME.get(envelope.path("type").asText());
            if (type == null) throw new IllegalArgumentException("Unknown room event type");
            return mapper.treeToValue(envelope.path("payload"), type);
        } catch (JsonProcessingException malformed) {
            throw new IllegalArgumentException("Malformed room event", malformed);
        }
    }
}
