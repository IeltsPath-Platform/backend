package com.ieltspath.commonstorage.supabase;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.SignedUpload;
import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.StorageException.Reason;
import com.ieltspath.commonstorage.StoredObject;
import com.ieltspath.commonstorage.config.SupabaseStorageProperties;
import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.commonstorage.upload.ContentTypes;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Cài {@link ObjectStorage} bằng REST của Supabase Storage.
 *
 * <p>Mọi kiểm tra đầu vào chạy trước khi gọi mạng. Lỗi từ Supabase đi qua {@link SupabaseErrors}.
 * Không retry: sau {@code outcomeUnknown} caller phải tự quyết định.
 *
 * <p>Ghép đường dẫn bằng chuỗi, không dùng biến URI: {@code DefaultUriBuilderFactory} mã hóa {@code /} thành
 * {@code %2F}, làm hỏng key nhiều đoạn. An toàn vì key và bucket đã bị giới hạn ký tự.
 */
public final class SupabaseObjectStorage implements ObjectStorage, AutoCloseable {

    private static final Pattern BUCKET = Pattern.compile("[a-z0-9][a-z0-9_-]{2,62}");
    private static final Duration MAX_TTL = Duration.ofMinutes(15);
    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP =
            new ParameterizedTypeReference<>() {
            };

    private final String baseUrl;
    private final RestClient rest;
    private final RestClient uploads;
    private final List<HttpClient> httpClients;

    /** Dùng cho test: nhận client đã cấu hình sẵn, không đóng gì. */
    SupabaseObjectStorage(String baseUrl, RestClient rest, RestClient uploads) {
        this(baseUrl, rest, uploads, List.of());
    }

    private SupabaseObjectStorage(String baseUrl, RestClient rest, RestClient uploads, List<HttpClient> httpClients) {
        this.baseUrl = baseUrl;
        this.rest = rest;
        this.uploads = uploads;
        this.httpClients = httpClients;
    }

    /** Dựng client thật. Yêu cầu {@link SupabaseStorageProperties#isConfigured()}. */
    public static SupabaseObjectStorage create(SupabaseStorageProperties props) {
        if (!props.isConfigured()) {
            throw new IllegalStateException("supabase storage is not configured");
        }
        HttpClient small = SupabaseRestClientFactory.httpClient(props);
        HttpClient large = SupabaseRestClientFactory.httpClient(props);
        RestClient rest = SupabaseRestClientFactory.client(props, small, props.readTimeout());
        RestClient uploads = SupabaseRestClientFactory.client(props, large, props.uploadTimeout());
        return new SupabaseObjectStorage(props.url() + "/storage/v1", rest, uploads, List.of(small, large));
    }

    @Override
    public StoredObject put(String bucket, String key, byte[] content, String contentType) {
        requireBucket("put", bucket);
        ObjectKeys.requireSafe(key);
        String mime = ContentTypes.normalize(contentType);
        if (content == null || content.length == 0 || mime == null) {
            throw misconfigured("put");
        }
        try {
            uploads.post()
                    .uri(objectPath(bucket, key))
                    .contentType(MediaType.parseMediaType(mime))
                    .header("x-upsert", "false")
                    .header("Cache-Control", "max-age=3600")
                    .body(content)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            throw SupabaseErrors.map("put", bucket, key, e, true);
        }
        return new StoredObject(bucket, key, mime, content.length);
    }

    @Override
    public void delete(String bucket, String key) {
        requireBucket("delete", bucket);
        ObjectKeys.requireSafe(key);
        try {
            rest.method(HttpMethod.DELETE)
                    .uri("/object/" + bucket)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("prefixes", List.of(key)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            throw SupabaseErrors.map("delete", bucket, key, e, true);
        }
    }

    /**
     * Dùng {@code GET /object/info}, không dùng {@code HEAD}: smoke cho thấy {@code HEAD} trả 400 không body cho cả
     * key không tồn tại, nên không phân biệt được.
     *
     * <p>404 từ {@code info} không đủ để kết luận "không tồn tại": khóa sai hoặc bucket sai cũng trả 404 trong body
     * (smoke 2026-10-19). Vì vậy 404 chỉ thành {@code false} khi {@code GET /bucket/{bucket}} xác nhận khóa và bucket
     * còn dùng được.
     */
    @Override
    public boolean exists(String bucket, String key) {
        requireBucket("exists", bucket);
        ObjectKeys.requireSafe(key);
        try {
            rest.get().uri("/object/info/" + bucket + "/" + key).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientResponseException e) {
            if (SupabaseErrors.effectiveStatus(e) != 404) {
                throw SupabaseErrors.map("exists", bucket, key, e, false);
            }
            confirmBucketReachable(bucket, key);
            return false;
        } catch (RuntimeException e) {
            throw SupabaseErrors.map("exists", bucket, key, e, false);
        }
    }

    private void confirmBucketReachable(String bucket, String key) {
        try {
            rest.get().uri("/bucket/" + bucket).retrieve().toBodilessEntity();
        } catch (RuntimeException e) {
            throw SupabaseErrors.map("exists", bucket, key, e, false);
        }
    }

    @Override
    public String publicUrl(String bucket, String key) {
        requireBucket("publicUrl", bucket);
        ObjectKeys.requireSafe(key);
        return baseUrl + "/object/public/" + bucket + "/" + key;
    }

    @Override
    public String signedDownloadUrl(String bucket, String key, Duration ttl) {
        requireBucket("signDownload", bucket);
        ObjectKeys.requireSafe(key);
        if (ttl == null || ttl.getSeconds() < 1 || ttl.compareTo(MAX_TTL) > 0) {
            throw misconfigured("signDownload");
        }
        Object signed = signedField("signDownload", bucket, key, "/object/sign/" + bucket + "/" + key,
                Map.of("expiresIn", ttl.getSeconds()), "signedURL");
        return baseUrl + signed;
    }

    @Override
    public SignedUpload signedUploadUrl(String bucket, String key) {
        requireBucket("signUpload", bucket);
        ObjectKeys.requireSafe(key);
        Object url = signedField("signUpload", bucket, key, "/object/upload/sign/" + bucket + "/" + key,
                Map.of(), "url");
        return new SignedUpload(baseUrl + url, key);
    }

    @Override
    public void close() {
        for (HttpClient client : httpClients) {
            client.close();
        }
    }

    /** Gửi POST ký URL và đọc đúng một trường. Thiếu hoặc sai dạng thì {@code UNEXPECTED_RESPONSE}. */
    private Object signedField(String operation, String bucket, String key, String path, Map<String, Object> body,
                               String field) {
        Map<String, Object> reply;
        try {
            reply = rest.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JSON_MAP);
        } catch (RuntimeException e) {
            throw SupabaseErrors.map(operation, bucket, key, e, false);
        }
        Object value = reply == null ? null : reply.get(field);
        if (!(value instanceof String text) || !text.startsWith("/")) {
            throw new StorageException(operation, Reason.UNEXPECTED_RESPONSE, 200, false,
                    StorageException.ref(bucket, key));
        }
        return text;
    }

    private String objectPath(String bucket, String key) {
        return "/object/" + bucket + "/" + key;
    }

    private static void requireBucket(String operation, String bucket) {
        if (bucket == null || !BUCKET.matcher(bucket).matches()) {
            throw misconfigured(operation);
        }
    }

    private static StorageException misconfigured(String operation) {
        return StorageException.withoutObject(operation, Reason.MISCONFIGURED);
    }
}
