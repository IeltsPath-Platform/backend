package com.ieltspath.game.infrastructure.websocket;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.game.application.result.GameRoomResult;
import com.ieltspath.game.application.result.RoomClosedEvent;
import com.ieltspath.game.application.usecase.ChangeGameRoomMembershipUseCase;
import com.ieltspath.game.application.usecase.GetGameRoomUseCase;
import com.ieltspath.game.application.usecase.GetGameMatchStateUseCase;
import com.ieltspath.game.application.usecase.GetWordlyRankingUseCase;
import com.ieltspath.game.application.usecase.RocketGameplayUseCase;
import com.ieltspath.game.application.usecase.SoloGameplayUseCase;
import com.ieltspath.game.application.usecase.StartGameMatchUseCase;
import com.ieltspath.game.application.usecase.SubmitGameAnswerUseCase;
import com.ieltspath.game.domain.repository.GameMatchPlayerRepository;
import com.ieltspath.game.domain.repository.GameMatchRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Two game instances share one Redis. A room event committed on instance A reaches the player connected to instance B,
 * and instance B delivers it through its own handler, the same path a local commit takes.
 */
@Testcontainers(disabledWithoutDocker = true)
class RoomEventAcrossInstancesTest {
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    final UUID room = UUID.randomUUID();
    final UUID host = UUID.randomUUID();
    final UUID guest = UUID.randomUUID();
    final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @Test
    void roomClosedCommittedOnOneInstanceReachesThePlayerOnAnother() throws Exception {
        var rooms = mock(GetGameRoomUseCase.class);
        var matches = mock(GameMatchRepository.class);
        var instanceB = new GameWebSocketHandler(json, rooms, mock(ChangeGameRoomMembershipUseCase.class),
                mock(StartGameMatchUseCase.class), mock(GetGameMatchStateUseCase.class),
                mock(SubmitGameAnswerUseCase.class), mock(SoloGameplayUseCase.class),
                mock(GetWordlyRankingUseCase.class), matches, mock(GameMatchPlayerRepository.class),
                mock(RocketGameplayUseCase.class));
        when(matches.findActiveIdByRoomId(room)).thenReturn(Optional.empty());
        when(rooms.byId(eq(room), any())).thenReturn(new GameRoomResult(room, "ABC123", host, "WORDLY", "VOCABULARY",
                "PRACTICE", 4, "WAITING", Instant.now(), List.of(), List.of()));

        var session = mock(WebSocketSession.class);
        List<String> received = new CopyOnWriteArrayList<>();
        when(session.getId()).thenReturn("guest-session");
        when(session.isOpen()).thenReturn(true);
        when(session.getAttributes()).thenReturn(Map.of(GameWebSocketHandler.ROOM_ID_ATTRIBUTE, room,
                GameWebSocketHandler.USER_ID_ATTRIBUTE, guest));
        doAnswer(call -> {
            received.add(((TextMessage) call.getArgument(0)).getPayload());
            return null;
        }).when(session).sendMessage(any());
        instanceB.afterConnectionEstablished(session);

        var template = redisTemplate();
        var codec = new RoomEventCodec(json);
        var bridgeOnB = new RoomEventBridge(codec, template, instanceB);
        var listenerOnB = listenerFor(template, bridgeOnB);
        var bridgeOnA = new RoomEventBridge(codec, template, event -> { });
        Thread.sleep(300);

        bridgeOnA.relay(new RoomClosedEvent(room, "Chủ phòng đã đóng phòng"));

        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (received.stream().noneMatch(payload -> payload.contains("ROOM_CLOSED"))
                && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        listenerOnB.stop();
        assertTrue(received.stream().anyMatch(payload -> payload.contains("ROOM_CLOSED")),
                "player on instance B did not receive the room closure: " + new ArrayList<>(received));
    }

    private StringRedisTemplate redisTemplate() {
        var factory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379)));
        factory.afterPropertiesSet();
        var template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();
        return template;
    }

    private RedisMessageListenerContainer listenerFor(StringRedisTemplate template, RoomEventBridge bridge)
            throws Exception {
        var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(template.getConnectionFactory());
        container.addMessageListener(bridge, new ChannelTopic(RoomEventBridge.CHANNEL));
        container.afterPropertiesSet();
        container.start();
        return container;
    }
}
