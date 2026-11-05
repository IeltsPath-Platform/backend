package com.ieltspath.game.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.ieltspath.game.application.port.CurrentPlayerProfile;
import com.ieltspath.game.application.port.WordlyWordProvider;
import com.ieltspath.game.application.result.RoomClosedEvent;
import com.ieltspath.game.application.result.RoomUpdatedEvent;
import com.ieltspath.game.application.usecase.ActiveWordlyGameUseCase;
import com.ieltspath.game.application.usecase.LobbySeatUseCase;
import com.ieltspath.game.infrastructure.config.WordlyConnectionRecovery;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** A lobby member who closes the last socket keeps the seat for 30 seconds, then leaves the room. */
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway", "game.rocket.round-scan-ms=3600000",
        "game.wordly.timeout-scan-ms=3600000", "game.lobby.seat-scan-ms=3600000"
})
@Testcontainers(disabledWithoutDocker = true)
@RecordApplicationEvents
class LobbySeatIntegrationTest {
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @Container static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", db::getJdbcUrl);
        registry.add("spring.datasource.username", db::getUsername);
        registry.add("spring.datasource.password", db::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired JdbcTemplate sql;
    @Autowired LobbySeatUseCase seats;
    @Autowired ActiveWordlyGameUseCase active;
    @Autowired com.ieltspath.game.application.usecase.ChangeGameRoomMembershipUseCase membership;
    @Autowired WordlyConnectionRecovery recovery;
    @Autowired ApplicationEvents events;
    @MockBean WordlyWordProvider words;
    @MockBean CurrentPlayerProfile profile;

    UUID room;
    UUID host;
    UUID guest;

    @BeforeEach
    void room() {
        room = UUID.randomUUID();
        host = UUID.randomUUID();
        guest = UUID.randomUUID();
        insertRoom(room, host, "MULTIPLAYER", "WAITING");
        member(room, host, "HOST", 1);
        member(room, guest, "PLAYER", 2);
    }

    private void insertRoom(UUID id, UUID hostId, String playerMode, String status) {
        sql.update("""
            INSERT INTO game_rooms(id,room_code,host_user_id,game_type,learning_domain,mode,max_players,config_snapshot,
                status,created_at,updated_at,player_mode,initial_lives,play_mode,word_limit)
            VALUES (?,upper(substring(md5(?::text),1,6)),?,'WORDLY','VOCABULARY','PRACTICE',?,'{}',?,now(),now(),?,4,'FINITE',10)
            """, id, id, hostId, "SOLO".equals(playerMode) ? 1 : 4, status, playerMode);
    }

    private void member(UUID roomId, UUID userId, String role, int slot) {
        sql.update("""
            INSERT INTO game_room_members(id,room_id,user_id,member_role,status,joined_at,ready_at,slot_number,display_name)
            VALUES (gen_random_uuid(),?,?,?,'READY',now(),now(),?,?)
            """, roomId, userId, role, slot, role.equals("HOST") ? "Host" : "Guest");
    }

    private String status(UUID userId) {
        return sql.queryForObject("SELECT status FROM game_room_members WHERE room_id=? AND user_id=?", String.class,
                room, userId);
    }

    private void passDeadline(UUID userId) {
        sql.update("UPDATE game_room_members SET reconnect_deadline_at=now()-interval '1 second' WHERE room_id=? AND user_id=?",
                room, userId);
    }

    @Test
    void closingTheLastSocketHoldsTheSeatForThirtySecondsWithoutReadiness() {
        seats.disconnected(room, guest);
        var row = sql.queryForMap("""
            SELECT status,ready_at,extract(epoch from reconnect_deadline_at-now()) AS left_s
            FROM game_room_members WHERE room_id=? AND user_id=?
            """, room, guest);
        assertEquals("DISCONNECTED", row.get("status"));
        assertNull(row.get("ready_at"));
        double secondsLeft = ((Number) row.get("left_s")).doubleValue();
        assertTrue(secondsLeft > 25 && secondsLeft <= 30, () -> "seconds left " + secondsLeft);
        assertTrue(events.stream(RoomUpdatedEvent.class).anyMatch(e -> e.room().id().equals(room)
                && e.room().members().stream().anyMatch(m -> m.userId().equals(guest) && m.status().equals("DISCONNECTED"))),
                "the lobby sees the member as disconnected");
        assertTrue(active.find(guest).isPresent(), "the seat is still offered while it is held");
    }

    @Test
    void reconnectingInTimeKeepsTheSeatNotReady() {
        seats.disconnected(room, guest);
        seats.reconnected(room, guest);
        assertEquals("JOINED", status(guest));
        assertNull(sql.queryForObject("SELECT reconnect_deadline_at FROM game_room_members WHERE room_id=? AND user_id=?",
                java.sql.Timestamp.class, room, guest));
    }

    @Test
    void anExpiredSeatIsReleasedWhenTheActiveGameIsChecked() {
        seats.disconnected(room, guest);
        passDeadline(guest);
        assertTrue(active.find(guest).isEmpty(), "an expired seat is never offered for rejoining");
        assertEquals("LEFT", status(guest));
        assertEquals("WAITING", sql.queryForObject("SELECT status FROM game_rooms WHERE id=?", String.class, room));
        seats.reconnected(room, guest);
        assertEquals("LEFT", status(guest), "reconnecting after the deadline does not bring the seat back");
    }

    @Test
    void anExpiredSeatDoesNotBlockJoiningAnotherRoom() {
        UUID other = UUID.randomUUID();
        insertRoom(other, UUID.randomUUID(), "MULTIPLAYER", "WAITING");
        sql.update("""
            INSERT INTO game_room_members(id,room_id,user_id,member_role,status,joined_at,slot_number)
            SELECT gen_random_uuid(),?,host_user_id,'HOST','JOINED',now(),1 FROM game_rooms WHERE id=?
            """, other, other);
        seats.disconnected(room, guest);
        passDeadline(guest);
        org.mockito.Mockito.when(profile.displayName()).thenReturn("Guest");
        var joined = membership.join(other, guest);
        assertTrue(joined.members().stream().anyMatch(m -> m.userId().equals(guest)));
        assertEquals("LEFT", status(guest));
    }

    @Test
    void theWorkerReleasesExpiredSeatsAndAnExpiredHostClosesTheRoom() {
        seats.disconnected(room, host);
        passDeadline(host);
        var due = seats.due(50);
        assertTrue(due.stream().anyMatch(s -> s.roomId().equals(room) && s.userId().equals(host)));
        for (var seat : due) seats.expire(seat.roomId(), seat.userId());
        assertEquals("CLOSED", sql.queryForObject("SELECT status FROM game_rooms WHERE id=?", String.class, room));
        assertTrue(events.stream(RoomClosedEvent.class).anyMatch(e -> e.roomId().equals(room)));
        seats.expire(room, host);
        assertTrue(active.find(guest).isEmpty(), "nobody is left in a closed room");
    }

    @Test
    void aRunningMatchKeepsItsOwnReconnectRules() {
        sql.update("UPDATE game_rooms SET status='IN_MATCH' WHERE id=?", room);
        seats.disconnected(room, guest);
        assertEquals("READY", status(guest));
    }

    @Test
    void aRestartGivesLobbyMembersTheGraceButLeavesSoloRoomsAlone() {
        UUID solo = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        insertRoom(solo, player, "SOLO", "WAITING");
        sql.update("""
            INSERT INTO game_room_members(id,room_id,user_id,member_role,status,joined_at,slot_number)
            VALUES (gen_random_uuid(),?,?,'HOST','JOINED',now(),1)
            """, solo, player);
        recovery.recover();
        assertEquals("DISCONNECTED", status(host));
        assertEquals("DISCONNECTED", status(guest));
        assertEquals("JOINED", sql.queryForObject("SELECT status FROM game_room_members WHERE room_id=?", String.class, solo));
        seats.reconnected(room, guest);
        assertEquals("JOINED", status(guest), "a member still connected restores the seat at the next heartbeat");
    }
}
