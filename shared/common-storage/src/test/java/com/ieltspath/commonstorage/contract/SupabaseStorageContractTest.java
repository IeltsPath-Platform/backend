package com.ieltspath.commonstorage.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.StorageException.Reason;
import com.ieltspath.commonstorage.config.SupabaseStorageProperties;
import com.ieltspath.commonstorage.supabase.SupabaseObjectStorage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Kiểm hợp đồng với Supabase thật. Mặc định bị skip.
 *
 * <p>Chạy khi có {@code STORAGE_CONTRACT_URL}, {@code STORAGE_CONTRACT_KEY_FILE} (đường dẫn file khóa ngoài repo)
 * và {@code STORAGE_CONTRACT_BUCKET} bắt đầu bằng {@code contract-test-}. Mọi thao tác nằm dưới
 * {@code contract-test/{uuid}/} trong bucket đó, và được dọn trong {@link #cleanUp()}. Không đọc khóa từ môi trường,
 * không in khóa.
 */
class SupabaseStorageContractTest {

    private static final String BUCKET_PREFIX = "contract-test-";

    private String projectUrl;
    private String bucket;
    private String secret;
    private String prefix;
    private SupabaseObjectStorage storage;
    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void setUp() throws IOException {
        projectUrl = System.getenv("STORAGE_CONTRACT_URL");
        String keyFile = System.getenv("STORAGE_CONTRACT_KEY_FILE");
        bucket = System.getenv("STORAGE_CONTRACT_BUCKET");
        assumeTrue(notBlank(projectUrl) && notBlank(keyFile) && notBlank(bucket),
                "contract test needs STORAGE_CONTRACT_URL, STORAGE_CONTRACT_KEY_FILE and STORAGE_CONTRACT_BUCKET");
        assumeTrue(bucket.startsWith(BUCKET_PREFIX), "contract test runs only in a contract-test-* bucket");
        secret = Files.readString(Path.of(keyFile), StandardCharsets.UTF_8).trim();
        assumeTrue(!secret.isEmpty(), "key file is empty");
        prefix = "contract-test/" + UUID.randomUUID() + "/";
        storage = SupabaseObjectStorage.create(new SupabaseStorageProperties(projectUrl, secret, null, null, null));
    }

    @AfterEach
    void cleanUp() {
        if (storage != null) {
            try {
                storage.delete(bucket, prefix + "png.png");
                storage.delete(bucket, prefix + "uploaded.png");
            } catch (RuntimeException ignored) {
                // Dọn dẹp chỉ là cố gắng. Object còn lại nằm dưới contract-test/ và bị xóa khi bucket bị xóa.
            }
            storage.close();
        }
    }

    @Test
    void writesReadsSignsAndDeletesOnlyInsideTheTestBucket() throws Exception {
        byte[] png = onePixelPng();
        String key = prefix + "png.png";

        // 1. Ghi, ghi trùng, kiểm tra tồn tại
        storage.put(bucket, key, png, "image/png");
        assertThatThrownBy(() -> storage.put(bucket, key, png, "image/png"))
                .isInstanceOfSatisfying(StorageException.class,
                        e -> assertThat(e.reason()).isEqualTo(Reason.ALREADY_EXISTS));
        assertThat(storage.exists(bucket, key)).isTrue();
        assertThat(storage.exists(bucket, prefix + "missing.png")).isFalse();

        // 2. Signed download: tải được, hết hạn sau TTL
        String url = storage.signedDownloadUrl(bucket, key, Duration.ofSeconds(2));
        HttpResponse<byte[]> first = get(url);
        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(first.body()).isEqualTo(png);
        Thread.sleep(3_000);
        assertThat(get(url).statusCode()).isGreaterThanOrEqualTo(400);

        // 3. Signed upload: client PUT trực tiếp, object xuất hiện
        String uploadKey = prefix + "uploaded.png";
        var upload = storage.signedUploadUrl(bucket, uploadKey);
        HttpResponse<String> put = http.send(HttpRequest.newBuilder(URI.create(upload.url()))
                        .header("Content-Type", "image/png")
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(png))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(put.statusCode()).isEqualTo(200);
        assertThat(storage.exists(bucket, uploadKey)).isTrue();

        // 4. Xóa bulk: object biến mất, xóa lần hai không lỗi
        storage.delete(bucket, key);
        assertThat(storage.exists(bucket, key)).isFalse();
        storage.delete(bucket, key);
    }

    @Test
    void wrongKeyIsRejectedWithoutLeakingIt() {
        String broken = secret.substring(0, secret.length() - 1) + (secret.endsWith("x") ? "y" : "x");
        try (SupabaseObjectStorage wrong = SupabaseObjectStorage.create(
                new SupabaseStorageProperties(projectUrl, broken, null, null, null))) {
            assertThatThrownBy(() -> wrong.exists(bucket, prefix + "any.png"))
                    .isInstanceOfSatisfying(StorageException.class, e -> {
                        assertThat(e.reason()).isIn(Reason.UNAUTHORIZED, Reason.FORBIDDEN, Reason.BAD_REQUEST);
                        assertThat(e.getMessage()).doesNotContain(broken).doesNotContain(secret);
                    });
        }
    }

    /** Boolean để thông báo lỗi không in khóa. */
    @Test
    void keyIsServerSecretNotJwt() {
        assertThat(secret.startsWith("sb_secret_")).as("secret key type").isTrue();
    }

    private HttpResponse<byte[]> get(String url) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static byte[] onePixelPng() {
        return java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=");
    }
}
