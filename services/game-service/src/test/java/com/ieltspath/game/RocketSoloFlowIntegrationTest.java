package com.ieltspath.game;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.game.application.port.CurrentPlayerProfile;
import com.ieltspath.game.application.port.WordlyWordProvider;
import com.ieltspath.game.application.result.MatchStartedEvent;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** The whole solo Rocket flow through the real HTTP layer, JPA mappings, migrations and constraints. */
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway", "game.rocket.round-scan-ms=3600000",
        "game.wordly.timeout-scan-ms=3600000"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@RecordApplicationEvents
class RocketSoloFlowIntegrationTest {
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

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate sql;
    @Autowired ApplicationEvents events;
    @MockBean WordlyWordProvider words;
    @MockBean CurrentPlayerProfile profile;

    private final Map<String, String> meanings = Map.of("ACTOR", "Diễn viên (nam)", "DRIVER", "Tài xế",
            "CAREER", "Sự nghiệp", "TEACHER", "Giáo viên", "DOCTOR", "Bác sĩ", "JOURNEY", "Hành trình");

    @Test
    void createReadyStartAnswerAndFinish() throws Exception {
        when(profile.displayName()).thenReturn("Rocket Tester");
        when(words.catalog()).thenReturn(meanings.entrySet().stream()
                .map(e -> new WordlyWordProvider.Word(UUID.nameUUIDFromBytes(e.getKey().getBytes()), e.getKey(), e.getValue()))
                .toList());
        Map<String, String> answers = meanings.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, e -> e.getKey().toLowerCase()));
        String token = token(UUID.randomUUID());

        JsonNode room = call(post("/api/games/rocket/rooms").content("{\"playerMode\":\"SOLO\",\"questionCount\":3}"), token, 201);
        assertEquals("ROCKET", room.get("gameType").asText());
        String roomId = room.get("id").asText();
        call(post("/api/games/rooms/" + roomId + "/ready").content("{\"ready\":true}"), token, 200);
        JsonNode match = call(post("/api/games/rooms/" + roomId + "/start"), token, 200);
        String matchId = match.get("matchId").asText();
        assertEquals("IN_PROGRESS", match.get("status").asText());
        assertEquals(3, sql.queryForObject("SELECT count(*) FROM game_rocket_rounds WHERE match_id=?::uuid", Integer.class, matchId));
        assertEquals(3, sql.queryForObject("SELECT question_count FROM game_matches WHERE id=?::uuid", Integer.class, matchId));
        assertEquals(List.of(new MatchStartedEvent(UUID.fromString(roomId), UUID.fromString(matchId))),
                events.stream(MatchStartedEvent.class).toList(), "the lobby is told to move into the match");

        JsonNode standing = call(get("/api/games/matches/" + matchId + "/ranking"), token, 200);
        assertFalse(standing.get("isFinal").asBoolean());
        assertEquals(1, standing.get("players").size());
        assertEquals("Rocket Tester", standing.get("players").get(0).get("displayName").asText());
        assertEquals(1, standing.get("players").get(0).get("rank").asInt());
        call(get("/api/games/matches/" + matchId + "/ranking"), token(UUID.randomUUID()), 404);

        JsonNode active = call(get("/api/games/me/active-game"), token, 200);
        assertEquals("ROCKET", active.get("gameType").asText());
        assertEquals(matchId, active.get("matchId").asText());

        for (int number = 1; number <= 3; number++) {
            JsonNode current = call(get("/api/games/rocket/matches/" + matchId + "/rounds/current"), token, 200);
            JsonNode round = current.get("round");
            if (round.get("deadlineAt").isNull()) {
                // The reveal pause has not passed yet; skip it as the worker would once it is due.
                sql.update("UPDATE game_rocket_rounds SET opens_at=now() WHERE id=?::uuid", round.get("roundId").asText());
                round = call(get("/api/games/rocket/matches/" + matchId + "/rounds/current"), token, 200).get("round");
            }
            assertEquals(number, round.get("number").asInt());
            assertFalse(round.toString().contains("correct"), "an open round never reveals its answer");
            String answer = answers.get(round.get("meaning").asText());
            String option = null;
            for (JsonNode candidate : round.get("options"))
                if (candidate.get("word").asText().equals(answer)) option = candidate.get("optionId").asText();
            JsonNode ack = call(post("/api/games/rocket/matches/" + matchId + "/rounds/" + round.get("roundId").asText() + "/answers")
                    .content("{\"requestId\":\"req-" + number + "\",\"optionId\":\"" + option + "\"}"), token, 200);
            assertTrue(ack.get("roundResult").get("correct").asBoolean());
            assertEquals(number == 3, ack.get("roundResult").get("matchFinished").asBoolean());
        }

        JsonNode finished = call(get("/api/games/rocket/matches/" + matchId + "/rounds/current"), token, 200);
        assertTrue(finished.get("matchFinished").asBoolean());
        assertEquals("ALL_ROUNDS_COMPLETED", finished.get("lastRoundResult").get("endReason").asText());
        assertEquals("WAITING", sql.queryForObject("SELECT status FROM game_rooms WHERE id=?::uuid", String.class, roomId));
        assertEquals(204, mvc.perform(get("/api/games/me/active-game").header("Authorization", token))
                .andReturn().getResponse().getStatus());
        JsonNode state = call(get("/api/games/matches/" + matchId), token, 200);
        assertEquals("SOLO", state.get("playerMode").asText());
        assertEquals(1, state.get("ownPlayer").get("rank").asInt());

        JsonNode ranking = call(get("/api/games/matches/" + matchId + "/ranking"), token, 200);
        assertTrue(ranking.get("isFinal").asBoolean());
        JsonNode me = ranking.get("players").get(0);
        assertEquals(1, me.get("rank").asInt());
        assertEquals(3, me.get("wordsSolved").asInt());
        assertEquals(finished.get("lastRoundResult").get("totalScore").asLong(), me.get("totalScore").asLong());
        call(get("/api/games/rocket/matches/" + matchId + "/rounds/current"), token(UUID.randomUUID()), 404);
    }

    @Test
    void resultLeaveRoutesAndCatalog() throws Exception {
        when(profile.displayName()).thenReturn("Rocket Tester");
        when(words.catalog()).thenReturn(meanings.entrySet().stream()
                .map(e -> new WordlyWordProvider.Word(UUID.nameUUIDFromBytes(e.getKey().getBytes()), e.getKey(), e.getValue()))
                .toList());
        String token = token(UUID.randomUUID());
        JsonNode catalog = call(get("/api/games/catalog"), token, 200);
        assertEquals("AVAILABLE", catalog.get("games").get(0).get("status").asText());
        assertEquals("ROCKET", catalog.get("games").get(0).get("gameType").asText());

        // Leave through the shared match route.
        String first = startSolo(token);
        assertEquals(409, mvc.perform(get("/api/games/matches/" + first + "/result").header("Authorization", token))
                .andReturn().getResponse().getStatus());
        JsonNode left = call(post("/api/games/matches/" + first + "/leave"), token, 200);
        assertEquals("PLAYER_LEFT", left.get("endReason").asText());
        assertEquals("FORFEITED", left.get("playerStatus").asText());
        JsonNode result = call(get("/api/games/matches/" + first + "/result"), token, 200);
        assertEquals("PLAYER_LEFT", result.get("endReason").asText());
        assertEquals(3, result.get("totalRounds").asInt());
        assertEquals(0, result.get("answered").asInt());

        // Leave through active-game, which a client uses when it finds the user still in a match.
        String second = startSolo(token);
        assertEquals(second, call(get("/api/games/me/active-game"), token, 200).get("matchId").asText());
        call(post("/api/games/me/active-game/leave"), token, 204);
        assertEquals(204, mvc.perform(get("/api/games/me/active-game").header("Authorization", token))
                .andReturn().getResponse().getStatus());
        assertEquals("PLAYER_LEFT", call(get("/api/games/matches/" + second + "/result"), token, 200).get("endReason").asText());
        call(get("/api/games/matches/" + second + "/result"), token(UUID.randomUUID()), 404);
    }

    @Test
    void multiplayerRoomCreateJoinEditAndStart() throws Exception {
        when(profile.displayName()).thenReturn("Rocket Tester");
        when(words.catalog()).thenReturn(meanings.entrySet().stream()
                .map(e -> new WordlyWordProvider.Word(UUID.nameUUIDFromBytes(e.getKey().getBytes()), e.getKey(), e.getValue()))
                .toList());
        String host = token(UUID.randomUUID());
        String guest = token(UUID.randomUUID());
        call(post("/api/games/rocket/rooms").content("{\"playerMode\":\"MULTIPLAYER\",\"questionCount\":3}"), host, 400);
        call(post("/api/games/rocket/rooms").content(
                "{\"playerMode\":\"MULTIPLAYER\",\"questionCount\":5,\"secondsPerQuestion\":12}"), host, 400);

        JsonNode room = call(post("/api/games/rocket/rooms").content("{\"playerMode\":\"MULTIPLAYER\",\"questionCount\":5}"), host, 201);
        assertEquals("MULTIPLAYER", room.get("playerMode").asText());
        assertEquals(4, room.get("maxPlayers").asInt());
        assertEquals(5, room.get("settings").get("questionCount").asInt());
        assertEquals(4, room.get("settings").get("initialLives").asInt());
        assertEquals(15, room.get("settings").get("secondsPerQuestion").asInt());
        assertTrue(room.get("settings").get("wordLimit").isNull());
        String roomId = room.get("id").asText();

        call(post("/api/games/rooms/code/" + room.get("roomCode").asText() + "/join"), guest, 200);
        call(post("/api/games/rooms/" + roomId + "/ready").content("{\"ready\":true}"), guest, 200);
        call(put("/api/games/rooms/" + roomId + "/settings").content("{\"initialLives\":3,\"questionCount\":6}"), guest, 403);
        JsonNode edited = call(put("/api/games/rooms/" + roomId + "/settings")
                .content("{\"initialLives\":3,\"questionCount\":6,\"secondsPerQuestion\":10}"), host, 200);
        assertEquals(6, edited.get("settings").get("questionCount").asInt());
        assertEquals(3, edited.get("settings").get("initialLives").asInt());
        assertEquals(10, edited.get("settings").get("secondsPerQuestion").asInt());
        assertTrue(edited.toString().contains("\"status\":\"JOINED\""), "a settings change un-readies the guest");
        // Out-of-range values are 400 (DTO bounds or the domain's IllegalArgumentException), never 500, and change nothing.
        for (String bad : List.of("{\"initialLives\":3,\"questionCount\":4}", "{\"initialLives\":3,\"questionCount\":21}",
                "{\"initialLives\":7,\"questionCount\":6}", "{\"initialLives\":3,\"secondsPerQuestion\":12}",
                "{\"initialLives\":3,\"secondsPerQuestion\":0}"))
            call(put("/api/games/rooms/" + roomId + "/settings").content(bad), host, 400);
        JsonNode unchanged = call(get("/api/games/rooms/" + roomId), host, 200).get("settings");
        assertEquals(6, unchanged.get("questionCount").asInt());
        assertEquals(10, unchanged.get("secondsPerQuestion").asInt());

        call(post("/api/games/rooms/" + roomId + "/ready").content("{\"ready\":true}"), guest, 200);
        call(post("/api/games/rooms/" + roomId + "/ready").content("{\"ready\":true}"), host, 200);
        String matchId = call(post("/api/games/rooms/" + roomId + "/start"), host, 200).get("matchId").asText();
        assertTrue(events.stream(MatchStartedEvent.class).anyMatch(e -> e.matchId().toString().equals(matchId)));
        var order = events.stream(Object.class).filter(e -> e instanceof MatchStartedEvent
                || e instanceof com.ieltspath.game.application.result.RocketRoundEvents.Started).map(e -> e.getClass().getSimpleName()).toList();
        assertEquals(List.of("MatchStartedEvent", "Started"), order.subList(order.size() - 2, order.size()),
                "the lobby hears MATCH_STARTED before the first round");

        for (String player : List.of(host, guest)) {
            JsonNode current = call(get("/api/games/rocket/matches/" + matchId + "/rounds/current"), player, 200);
            assertEquals(10, current.get("settings").get("secondsPerQuestion").asInt());
            assertEquals(3, current.get("round").get("player").get("livesRemaining").asInt());
        }
        assertEquals(2, call(get("/api/games/matches/" + matchId + "/ranking"), guest, 200).get("players").size());
        assertEquals(6, sql.queryForObject("SELECT count(*) FROM game_rocket_rounds WHERE match_id=?::uuid", Integer.class, matchId));
    }

    private String startSolo(String token) throws Exception {
        String roomId = call(post("/api/games/rocket/rooms").content("{\"playerMode\":\"SOLO\",\"questionCount\":3}"), token, 201)
                .get("id").asText();
        call(post("/api/games/rooms/" + roomId + "/ready").content("{\"ready\":true}"), token, 200);
        return call(post("/api/games/rooms/" + roomId + "/start"), token, 200).get("matchId").asText();
    }

    private JsonNode call(MockHttpServletRequestBuilder request, String token, int expected) throws Exception {
        var response = mvc.perform(request.header("Authorization", token).contentType("application/json"))
                .andReturn().getResponse();
        String body = response.getContentAsString();
        assertEquals(expected, response.getStatus(), body);
        return body.isEmpty() ? json.nullNode() : json.readTree(body);
    }

    private static String token(UUID user) {
        var claims = JwtClaimsSet.builder().issuer("urn:code-base:api-gateway").subject(user.toString())
                .expiresAt(Instant.now().plusSeconds(300)).claim("roles", List.of("CUSTOMER")).build();
        var key = new OctetSequenceKey.Builder(KEY).build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
