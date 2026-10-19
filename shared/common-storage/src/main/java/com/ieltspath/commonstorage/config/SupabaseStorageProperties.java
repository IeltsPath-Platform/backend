package com.ieltspath.commonstorage.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình kết nối Supabase Storage.
 *
 * <p>{@code url} và {@code secretKey} phải cùng trống hoặc cùng có. Cả hai trống nghĩa là chưa cấu hình:
 * storage trả lỗi khi dùng (xem {@code UnavailableObjectStorage}). Chỉ một trong hai trống là lỗi cấu hình.
 *
 * <p>{@link #toString()} che khóa, vì Spring có thể in properties khi báo lỗi binding.
 */
@ConfigurationProperties(prefix = "app.storage.supabase")
public record SupabaseStorageProperties(
        String url,
        String secretKey,
        Duration connectTimeout,
        Duration readTimeout,
        Duration uploadTimeout
) {

    private static final Duration DEFAULT_CONNECT = Duration.ofSeconds(3);
    private static final Duration DEFAULT_READ = Duration.ofSeconds(10);
    private static final Duration DEFAULT_UPLOAD = Duration.ofSeconds(120);

    public SupabaseStorageProperties {
        url = blankToNull(url);
        secretKey = blankToNull(secretKey);
        if ((url == null) != (secretKey == null)) {
            throw new IllegalArgumentException(
                    "app.storage.supabase.url and app.storage.supabase.secret-key must be configured together");
        }
        if (url != null) {
            url = normalizeUrl(url);
        }
        connectTimeout = positiveOrDefault(connectTimeout, DEFAULT_CONNECT, "connect-timeout");
        readTimeout = positiveOrDefault(readTimeout, DEFAULT_READ, "read-timeout");
        uploadTimeout = positiveOrDefault(uploadTimeout, DEFAULT_UPLOAD, "upload-timeout");
    }

    /** true khi đã cấu hình đủ url và khóa. */
    public boolean isConfigured() {
        return url != null;
    }

    /** Không in khóa. Thông báo lỗi của các exception cũng không bao giờ chứa giá trị này. */
    @Override
    public String toString() {
        return "SupabaseStorageProperties[url=" + url + ", secretKey=" + (secretKey == null ? "null" : "***")
                + ", connectTimeout=" + connectTimeout + ", readTimeout=" + readTimeout
                + ", uploadTimeout=" + uploadTimeout + "]";
    }

    /**
     * Chấp nhận {@code https://} bất kỳ, hoặc {@code http://} chỉ khi host là loopback. So sánh bằng
     * {@link URI#getHost()}, không so chuỗi: {@code http://localhost@evil.com} có host {@code evil.com}.
     * Từ chối userinfo, query, fragment, path và {@code /storage/v1} (đã có sẵn trong baseUrl).
     */
    static String normalizeUrl(String raw) {
        URI uri;
        try {
            uri = new URI(raw.trim());
        } catch (java.net.URISyntaxException e) {
            throw new IllegalArgumentException("app.storage.supabase.url is not a valid URL");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(java.util.Locale.ROOT);
        String host = uri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("app.storage.supabase.url must include a host");
        }
        boolean loopback = host.equals("localhost") || host.equals("127.0.0.1") || host.equals("[::1]")
                || host.equals("::1");
        if (!"https".equals(scheme) && !("http".equals(scheme) && loopback)) {
            throw new IllegalArgumentException("app.storage.supabase.url must use https (http only for localhost)");
        }
        if (uri.getRawUserInfo() != null) {
            throw new IllegalArgumentException("app.storage.supabase.url must not contain user info");
        }
        if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("app.storage.supabase.url must not contain a query or fragment");
        }
        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (!path.isEmpty()) {
            throw new IllegalArgumentException("app.storage.supabase.url must be the project root without a path");
        }
        return scheme + "://" + uri.getRawAuthority();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Duration positiveOrDefault(Duration value, Duration fallback, String name) {
        if (value == null) {
            return fallback;
        }
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("app.storage.supabase." + name + " must be positive");
        }
        return value;
    }
}
