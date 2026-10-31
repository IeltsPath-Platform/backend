package com.ieltspath.content.infrastructure.config;

import com.ieltspath.content.domain.vo.MediaReferencePolicy;
import com.ieltspath.content.application.usecase.MediaUrlResolver;
import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.UnavailableObjectStorage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ContentMediaConfig {

    /** {@code content.media.base-url} is the public bucket prefix for audio keys; blank disables key resolution. */
    @Bean
    public MediaReferencePolicy mediaReferencePolicy(@Value("${content.media.base-url:}") String baseUrl) {
        return new MediaReferencePolicy(baseUrl);
    }

    @Bean
    public MediaUrlResolver mediaUrlResolver(MediaReferencePolicy publicMedia,
                                             ObjectProvider<ObjectStorage> storage,
                                             @Value("${content.media.premium-bucket:media-premium}") String bucket,
                                             @Value("${content.media.premium-url-ttl-seconds:900}") long ttlSeconds) {
        if (ttlSeconds < 1 || ttlSeconds > 900) {
            throw new IllegalArgumentException("Premium media URL TTL must be between 1 and 900 seconds");
        }
        return new MediaUrlResolver(publicMedia, storage.getIfAvailable(UnavailableObjectStorage::new), bucket,
                Duration.ofSeconds(ttlSeconds));
    }
}
