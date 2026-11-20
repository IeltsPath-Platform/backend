package com.ieltspath.content.infrastructure.config;

import com.ieltspath.content.domain.service.CodePattern;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CodePatternConfiguration {
    @Bean
    CodePattern codePattern() { return new CodePattern(); }
}
