package com.ieltspath.commonstorage.config;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.UnavailableObjectStorage;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * Đăng ký {@link ObjectStorage}. Tắt mặc định: service phải bật {@code app.storage.enabled=true}.
 *
 * <p>Phase hiện tại chỉ đăng ký bản {@link UnavailableObjectStorage}. Client Supabase được thêm sau khi smoke
 * test với Supabase thật hoàn tất.
 */
@AutoConfiguration
@ConditionalOnClass(RestClient.class)
@ConditionalOnProperty(prefix = "app.storage", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(SupabaseStorageProperties.class)
public class CommonStorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ObjectStorage.class)
    ObjectStorage objectStorage() {
        return new UnavailableObjectStorage();
    }
}
