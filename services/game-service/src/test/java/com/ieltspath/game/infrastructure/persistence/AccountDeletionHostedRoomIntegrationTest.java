package com.ieltspath.game.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ieltspath.game.api.dto.response.GameRoomResponse;
import com.ieltspath.game.application.port.CurrentPlayerProfile;
import com.ieltspath.game.application.port.WordlyWordProvider;
import com.ieltspath.game.application.usecase.GetGameRoomUseCase;
import com.ieltspath.game.domain.aggregate.GameRoom;
import com.ieltspath.game.domain.repository.GameRoomRepository;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** A room whose host deleted their account still loads, closed, for the players left in it. */
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway", "game.rocket.round-scan-ms=3600000",
        "game.wordly.timeout-scan-ms=3600000", "game.lobby.seat-scan-ms=3600000",
        "game.user-deletion.poll-delay-ms=3600000"
})
@Testcontainers(disabledWithoutDocker = true)
class AccountDeletionHostedRoomIntegrationTest {
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
    @Autowired GameRoomRepository rooms;
    @Autowired GetGameRoomUseCase getRoom;
    @MockBean WordlyWordProvider words;
    @MockBean CurrentPlayerProfile profile;

    @Test
    void aRoomHostedByADeletedUserIsClosedAndStillLoads() {
        UUID room = UUID.randomUUID();
        UUID host = UUID.randomUUID();
        UUID guest = UUID.randomUUID();
        insertRoom(room, host);
        member(room, host, "HOST", 1);
        member(room, guest, "PLAYER", 2);
        UUID otherRoom = UUID.randomUUID();
        insertRoom(otherRoom, guest);
        member(otherRoom, guest, "HOST", 1);
        member(otherRoom, host, "PLAYER", 2);

        new JdbcDeletedUserStore(sql).deleteUserGameData(host);

        GameRoom reloaded = rooms.findById(room).orElseThrow();
        assertThat(reloaded.status()).isEqualTo(GameRoom.GameRoomStatus.CLOSED);
        assertThat(reloaded.hostUserId()).isEqualTo(JdbcDeletedUserStore.DELETED_USER);
        GameRoom.Member formerHost = reloaded.members().get(JdbcDeletedUserStore.DELETED_USER);
        assertThat(formerHost.role()).isEqualTo(GameRoom.MemberRole.HOST);
        assertThat(formerHost.status()).isEqualTo(GameRoom.MemberStatus.LEFT);
        assertThat(formerHost.displayName()).isNull();

        GameRoomResponse response = GameRoomResponse.from(getRoom.byId(room, guest));
        assertThat(response.status()).isEqualTo("CLOSED");
        assertThat(rooms.findByCode(reloaded.roomCode())).isPresent();

        // A room they only played in keeps its host and loses just their seat.
        GameRoom joined = rooms.findById(otherRoom).orElseThrow();
        assertThat(joined.status()).isEqualTo(GameRoom.GameRoomStatus.WAITING);
        assertThat(joined.members()).containsOnlyKeys(guest);
        assertThat(sql.queryForObject("SELECT count(*) FROM game_room_members WHERE user_id = ?", Integer.class, host))
                .isZero();
    }

    private void insertRoom(UUID id, UUID hostId) {
        sql.update("""
            INSERT INTO game_rooms(id,room_code,host_user_id,game_type,learning_domain,mode,max_players,config_snapshot,
                status,created_at,updated_at,player_mode,initial_lives,play_mode,word_limit)
            VALUES (?,upper(substring(md5(?::text),1,6)),?,'WORDLY','VOCABULARY','PRACTICE',4,'{}','WAITING',now(),now(),
                'MULTIPLAYER',4,'FINITE',10)
            """, id, id, hostId);
    }

    private void member(UUID roomId, UUID userId, String role, int slot) {
        sql.update("""
            INSERT INTO game_room_members(id,room_id,user_id,member_role,status,joined_at,ready_at,slot_number,display_name)
            VALUES (gen_random_uuid(),?,?,?,'READY',now(),now(),?,?)
            """, roomId, userId, role, slot, role.equals("HOST") ? "Host" : "Guest");
    }
}
