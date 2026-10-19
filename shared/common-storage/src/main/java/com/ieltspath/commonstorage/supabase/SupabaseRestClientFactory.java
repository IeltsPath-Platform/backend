package com.ieltspath.commonstorage.supabase;

import com.ieltspath.commonstorage.config.SupabaseStorageProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Dựng {@link RestClient} gọi Supabase Storage.
 *
 * <p>Khóa {@code sb_secret_…} gửi ở header {@code apikey}, không gửi ở {@code Authorization}, vì khóa này không phải
 * JWT (smoke 2026-10-19 xác nhận: {@code Bearer} trả 400). Khóa legacy dạng JWT thì gửi cả hai.
 *
 * <p>Dùng {@link HttpClient} của JDK vì {@code DELETE} bulk có body; không theo redirect, để khóa không bị gửi tới
 * host khác.
 */
final class SupabaseRestClientFactory {

    private static final String STORAGE_PATH = "/storage/v1";
    private static final String SECRET_PREFIX = "sb_secret_";

    private SupabaseRestClientFactory() {
    }

    /** Gắn baseUrl và header xác thực. Tách riêng để test kiểm header mà không cần mạng. */
    static RestClient.Builder configure(RestClient.Builder builder, SupabaseStorageProperties props) {
        builder.baseUrl(props.url() + STORAGE_PATH).defaultHeader("apikey", props.secretKey());
        if (!props.secretKey().startsWith(SECRET_PREFIX)) {
            builder.defaultHeader("Authorization", "Bearer " + props.secretKey());
        }
        return builder;
    }

    static HttpClient httpClient(SupabaseStorageProperties props) {
        return HttpClient.newBuilder()
                .connectTimeout(props.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    static RestClient client(SupabaseStorageProperties props, HttpClient http, Duration readTimeout) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(readTimeout);
        return configure(RestClient.builder().requestFactory(factory), props).build();
    }
}
