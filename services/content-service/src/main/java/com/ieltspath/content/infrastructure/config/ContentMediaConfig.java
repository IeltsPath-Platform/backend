package com.ieltspath.content.infrastructure.config;

import com.ieltspath.content.domain.vo.MediaReferencePolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContentMediaConfig {

    /** {@code content.media.base-url} is the public bucket prefix for audio keys; blank disables key resolution. */
    @Bean
    public MediaReferencePolicy mediaReferencePolicy(@Value("${content.media.base-url:}") String baseUrl) {
        return new MediaReferencePolicy(baseUrl);
    }
}
