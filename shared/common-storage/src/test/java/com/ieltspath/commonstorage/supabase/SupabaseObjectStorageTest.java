package com.ieltspath.commonstorage.supabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.StorageException.Reason;
import com.ieltspath.commonstorage.config.SupabaseStorageProperties;
import com.ieltspath.commonstorage.key.InvalidObjectKeyException;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SupabaseObjectStorageTest {

    private static final String KEY = "sb_secret_test-not-real";
    private static final String BASE = "https://test-ref.supabase.co/storage/v1";
    private static final UUID USER = UUID.fromString("0b1f2a3c-4d5e-4f60-8172-93a4b5c6d7e8");
    private static final String OBJECT_KEY = "speaking/" + USER + "/9c2e4b1a-0000-4000-8000-000000000001.webm";

    private MockRestServiceServer server;
    private SupabaseObjectStorage storage;

    @BeforeEach
    void setUp() {
        SupabaseStorageProperties props = new SupabaseStorageProperties("https://test-ref.supabase.co", KEY,
                null, null, null);
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = SupabaseRestClientFactory.configure(builder, props).build();
        storage = new SupabaseObjectStorage(BASE, client, client);
    }

    private StorageException.Reason reasonOf(Runnable call) {
        try {
            call.run();
            throw new AssertionError("expected StorageException");
        } catch (StorageException e) {
            return e.reason();
        }
    }

    @Test
    void putPostsUnencodedPathWithUpsertOffAndNormalisedType() {
        server.expect(requestTo(BASE + "/object/speaking-recordings/" + OBJECT_KEY))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-upsert", "false"))
                .andExpect(header("apikey", KEY))
                .andExpect(header("Content-Type", "audio/webm"))
                .andExpect(header("Cache-Control", "max-age=3600"))
                .andRespond(withSuccess("{\"Id\":\"x\",\"Key\":\"k\"}", MediaType.APPLICATION_JSON));

        byte[] body = {1, 2, 3, 4};
        var stored = storage.put("speaking-recordings", OBJECT_KEY, body, "audio/webm;codecs=opus");

        assertThat(stored.sizeBytes()).isEqualTo(4);
        assertThat(stored.contentType()).isEqualTo("audio/webm");
        server.verify();
    }

    @Test
    void putDuplicateMapsBodyStatusNotHttpStatus() {
        server.expect(requestTo(BASE + "/object/media/a/b.png"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"statusCode\":\"409\",\"error\":\"Duplicate\",\"message\":\"The resource already exists\"}"));

        assertThatThrownBy(() -> storage.put("media", "a/b.png", new byte[]{1}, "image/png"))
                .isInstanceOfSatisfying(StorageException.class, e -> {
                    assertThat(e.reason()).isEqualTo(Reason.ALREADY_EXISTS);
                    assertThat(e.status()).isEqualTo(409);
                    assertThat(e.outcomeUnknown()).isFalse();
                    assertThat(e.getMessage()).doesNotContain("already exists").doesNotContain("a/b.png");
                });
    }

    @Test
    void putTimeoutReportsOutcomeUnknown() {
        server.expect(requestTo(BASE + "/object/media/a/b.png"))
                .andRespond(request -> {
                    throw new IOException("boom");
                });

        assertThatThrownBy(() -> storage.put("media", "a/b.png", new byte[]{1}, "image/png"))
                .isInstanceOfSatisfying(StorageException.class, e -> {
                    assertThat(e.reason()).isEqualTo(Reason.UNAVAILABLE);
                    assertThat(e.outcomeUnknown()).isTrue();
                    assertThat(e.getCause()).isNull();
                });
    }

    @Test
    void deleteUsesBulkPrefixesBody() {
        server.expect(requestTo(BASE + "/object/speaking-recordings"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(content().json("{\"prefixes\":[\"" + OBJECT_KEY + "\"]}"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        storage.delete("speaking-recordings", OBJECT_KEY);
        server.verify();
    }

    @Test
    void existsUsesInfoEndpointAndBodyStatusFor404() {
        server.expect(requestTo(BASE + "/object/info/media/a/b.png"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"statusCode\":\"404\",\"error\":\"not_found\",\"message\":\"Object not found\"}"));
        server.expect(requestTo(BASE + "/bucket/media"))
                .andRespond(withSuccess("{\"id\":\"media\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/object/info/media/a/b.png"))
                .andRespond(withSuccess("{\"name\":\"a/b.png\"}", MediaType.APPLICATION_JSON));

        assertThat(storage.exists("media", "a/b.png")).isFalse();
        assertThat(storage.exists("media", "a/b.png")).isTrue();
    }

    @Test
    void notFoundIsNotTrustedWhenCredentialsOrBucketFail() {
        server.expect(requestTo(BASE + "/object/info/media/a/b.png"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"statusCode\":\"404\",\"message\":\"Bucket not found\"}"));
        server.expect(requestTo(BASE + "/bucket/media"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"statusCode\":\"400\",\"error\":\"Error\"}"));

        assertThat(reasonOf(() -> storage.exists("media", "a/b.png"))).isEqualTo(Reason.BAD_REQUEST);
    }

    @Test
    void missingBucketIsNotAMissingObject() {
        server.expect(requestTo(BASE + "/object/info/media/a/b.png"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"statusCode\":\"404\"}"));
        server.expect(requestTo(BASE + "/bucket/media"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"statusCode\":\"404\",\"message\":\"Bucket not found\"}"));

        assertThat(reasonOf(() -> storage.exists("media", "a/b.png"))).isEqualTo(Reason.NOT_FOUND);
    }

    @Test
    void publicUrlIsPureAndKeepsLegacyUppercaseKeys() {
        assertThat(storage.publicUrl("media", "listening/demo/numM.mp3"))
                .isEqualTo(BASE + "/object/public/media/listening/demo/numM.mp3");
        server.verify();
    }

    @Test
    void signedDownloadUrlIsAbsoluteAndUsesRequestedTtl() {
        server.expect(requestTo(BASE + "/object/sign/speaking-recordings/" + OBJECT_KEY))
                .andExpect(content().json("{\"expiresIn\":300}"))
                .andRespond(withSuccess("{\"signedURL\":\"/object/sign/speaking-recordings/" + OBJECT_KEY
                        + "?token=abc\"}", MediaType.APPLICATION_JSON));

        assertThat(storage.signedDownloadUrl("speaking-recordings", OBJECT_KEY, Duration.ofMinutes(5)))
                .isEqualTo(BASE + "/object/sign/speaking-recordings/" + OBJECT_KEY + "?token=abc");
    }

    @Test
    void signedUploadUrlHasNoUpsertAndReturnsAbsoluteUrl() {
        server.expect(requestTo(BASE + "/object/upload/sign/media/listening/x.mp3"))
                .andExpect(content().json("{}"))
                .andRespond(withSuccess("{\"url\":\"/object/upload/sign/media/listening/x.mp3?token=t\"}",
                        MediaType.APPLICATION_JSON));

        var upload = storage.signedUploadUrl("media", "listening/x.mp3");

        assertThat(upload.url()).isEqualTo(BASE + "/object/upload/sign/media/listening/x.mp3?token=t");
        assertThat(upload.key()).isEqualTo("listening/x.mp3");
    }

    @Test
    void unexpectedSignedResponseIsStorageException() {
        server.expect(requestTo(BASE + "/object/sign/media/a/b.png"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(reasonOf(() -> storage.signedDownloadUrl("media", "a/b.png", Duration.ofMinutes(1))))
                .isEqualTo(Reason.UNEXPECTED_RESPONSE);
    }

    @Test
    void htmlBodyInsteadOfJsonIsUnexpectedResponse() {
        server.expect(requestTo(BASE + "/object/sign/media/a/b.png"))
                .andRespond(withSuccess("<html>502</html>", MediaType.TEXT_HTML));

        assertThat(reasonOf(() -> storage.signedDownloadUrl("media", "a/b.png", Duration.ofMinutes(1))))
                .isIn(Reason.UNEXPECTED_RESPONSE, Reason.UPSTREAM_ERROR);
    }

    @Test
    void httpErrorsMapToReasons() {
        server.expect(requestTo(BASE + "/object/info/media/a/b.png"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"message\":\"nope\"}"));
        server.expect(requestTo(BASE + "/object/info/media/a/b.png"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("{}"));
        server.expect(requestTo(BASE + "/object/info/media/a/b.png"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("<html/>"));

        assertThat(reasonOf(() -> storage.exists("media", "a/b.png"))).isEqualTo(Reason.UNAUTHORIZED);
        assertThat(reasonOf(() -> storage.exists("media", "a/b.png"))).isEqualTo(Reason.RATE_LIMITED);
        assertThat(reasonOf(() -> storage.exists("media", "a/b.png"))).isEqualTo(Reason.UPSTREAM_ERROR);
    }

    @Test
    void badInputFailsBeforeAnyRequest() {
        assertThatThrownBy(() -> storage.put("media", "a/../b", new byte[]{1}, "image/png"))
                .isInstanceOf(InvalidObjectKeyException.class);
        assertThat(reasonOf(() -> storage.put("A", "a/b", new byte[]{1}, "image/png")))
                .isEqualTo(Reason.MISCONFIGURED);
        assertThat(reasonOf(() -> storage.put("media", "a/b", new byte[0], "image/png")))
                .isEqualTo(Reason.MISCONFIGURED);
        assertThat(reasonOf(() -> storage.put("media", "a/b", new byte[]{1}, " ")))
                .isEqualTo(Reason.MISCONFIGURED);
        assertThat(reasonOf(() -> storage.signedDownloadUrl("media", "a/b", Duration.ZERO)))
                .isEqualTo(Reason.MISCONFIGURED);
        assertThat(reasonOf(() -> storage.signedDownloadUrl("media", "a/b", Duration.ofMinutes(16))))
                .isEqualTo(Reason.MISCONFIGURED);
        server.verify();
    }

    @Test
    void errorsNeverContainKeyBucketOrSecret() {
        server.expect(requestTo(BASE + "/object/info/media/" + OBJECT_KEY))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"statusCode\":\"403\",\"message\":\"" + OBJECT_KEY + " " + KEY + "\"}"));

        assertThatThrownBy(() -> storage.exists("media", OBJECT_KEY))
                .isInstanceOfSatisfying(StorageException.class, e -> {
                    assertThat(e.getMessage()).doesNotContain(KEY).doesNotContain(USER.toString())
                            .doesNotContain("media").doesNotContain(OBJECT_KEY);
                    assertThat(e.getCause()).isNull();
                });
    }
}
