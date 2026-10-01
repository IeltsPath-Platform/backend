package com.group01.assessment.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.group01.assessment.application.exception.ContentUnavailableException;
import com.group01.assessment.application.port.ContentPackageProvider;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Reads Content's internal package-version contract over real HTTP from a local stub server. */
class ContentPackageClientTest {
    private static final UUID VERSION = UUID.fromString("20000000-0000-4000-8000-000000000401");

    private HttpServer server;
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<Integer> status = new AtomicReference<>(200);
    private final AtomicReference<String> body = new AtomicReference<>("");

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/learning-content/package-versions/", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = body.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status.get(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.start();
        Jwt jwt = Jwt.withTokenValue("internal-token").header("alg", "HS256").subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
        SecurityContextHolder.clearContext();
    }

    private ContentPackageClient client() {
        return new ContentPackageClient(RestClient.builder(), "http://127.0.0.1:" + server.getAddress().getPort());
    }

    @Test
    void readsTheContractShapeAndForwardsTheBearer() {
        body.set("""
                {"packageVersionId":"%s","packageId":"20000000-0000-4000-8000-000000000301","packageType":"TOPIC_TEST",
                 "topicId":"10000000-0000-4000-8000-000000000001","rules":{},
                 "sections":[{"sectionId":"20000000-0000-4000-8000-000000000701","title":"Street trees","skill":"READING",
                   "instructions":null,"sortOrder":1,"passage":"A. City trees.",
                   "audio":{"assetId":"20000000-0000-4000-8000-000000000702","mediaUrl":"https://cdn/a.mp3","durationSeconds":60,"transcript":"secret"},
                   "items":[
                     {"questionVersionId":"20000000-0000-4000-8000-000000000002","sortOrder":1,"stem":"Main idea?",
                      "options":[{"optionKey":"A","content":"Oaks","sortOrder":1},{"optionKey":"B","content":"Benefits","sortOrder":2}],
                      "answerSpec":{"type":"CHOICE","correct":"B"},"explanation":"All paragraphs.","maxScore":1.0,
                      "knowledgePointMappings":[{"knowledgePointId":"10000000-0000-4000-8000-000000000002","weight":1.0}]},
                     {"questionVersionId":"20000000-0000-4000-8000-000000000012","sortOrder":2,"stem":"People who doubt?",
                      "options":null,"answerSpec":{"type":"FILL","accepted":["critics"]},"explanation":null,"maxScore":1.0,
                      "knowledgePointMappings":[]}]}]}
                """.formatted(VERSION));

        ContentPackageProvider.PackageVersion version = client().findPackageVersion(VERSION).orElseThrow();

        assertEquals("Bearer internal-token", authorization.get());
        assertEquals("TOPIC_TEST", version.packageType());
        ContentPackageProvider.Section section = version.sections().getFirst();
        assertEquals("A. City trees.", section.passage());
        var audio = new ObjectMapper().valueToTree(section).path("audio");
        assertEquals("20000000-0000-4000-8000-000000000702", audio.path("assetId").asText());
        assertEquals("https://cdn/a.mp3", audio.path("mediaUrl").asText());
        assertEquals(60, audio.path("durationSeconds").asInt());
        assertEquals("secret", audio.path("transcript").asText());
        ContentPackageProvider.Item choice = section.items().get(0);
        assertEquals(Map.of("type", "CHOICE", "correct", "B"), choice.answerSpec());
        assertEquals("B", choice.options().get(1).optionKey());
        assertEquals(0, new BigDecimal("1.0").compareTo(choice.knowledgePoints().getFirst().weight()));
        ContentPackageProvider.Item fill = section.items().get(1);
        assertNull(fill.options());
        assertEquals(List.of("critics"), fill.answerSpec().get("accepted"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"assetId\":\"20000000-0000-4000-8000-000000000702\",\"mediaUrl\":\"\"}",
            "{\"assetId\":\"20000000-0000-4000-8000-000000000702\",\"mediaUrl\":\"https://cdn/a.mp3\",\"durationSeconds\":-1}"
    })
    void rejectsMalformedAudio(String audio) {
        body.set("""
                {"packageVersionId":"%s","packageType":"TOPIC_TEST","sections":[{
                  "sectionId":"20000000-0000-4000-8000-000000000701","audio":%s,"items":[]}]}
                """.formatted(VERSION, audio));
        assertThrows(ContentUnavailableException.class, () -> client().findPackageVersion(VERSION));
    }

    @Test
    void acceptsSectionsWithoutAudioAndAudioWithOptionalMetadata() {
        String section = """
                {"sectionId":"20000000-0000-4000-8000-000000000701","items":[]%s}
                """;
        for (String field : List.of("", ",\"audio\":null", """
                ,"audio":{"assetId":"20000000-0000-4000-8000-000000000702",
                          "mediaUrl":"https://cdn/a.mp3","durationSeconds":null,"transcript":null}
                """)) {
            body.set("{\"packageVersionId\":\"" + VERSION + "\",\"packageType\":\"TOPIC_TEST\",\"sections\":["
                    + section.formatted(field) + "]}");
            assertTrue(client().findPackageVersion(VERSION).isPresent());
        }
    }

    @Test
    void unknownVersionIsEmptyAndAnyOtherFailureIsUnavailable() {
        status.set(404);
        assertTrue(client().findPackageVersion(VERSION).isEmpty());

        status.set(500);
        assertThrows(ContentUnavailableException.class, () -> client().findPackageVersion(VERSION));

        status.set(200);
        body.set("{\"packageVersionId\":\"" + UUID.randomUUID() + "\",\"packageType\":\"QUIZ\",\"sections\":[]}");
        assertThrows(ContentUnavailableException.class, () -> client().findPackageVersion(VERSION),
                "a response for another version is rejected");
    }
}
