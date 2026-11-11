package com.ieltspath.commonllm;

import com.ieltspath.commonresilience.CommonResilienceAutoConfiguration;
import com.ieltspath.commonresilience.ResilientCall;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration(after = CommonResilienceAutoConfiguration.class)
public class CommonLlmAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    LlmClientFactory llmClientFactory(RestClient.Builder builder, ResilientCall resilientCall,
                                     MeterRegistry registry) {
        return new LlmClientFactory(builder, resilientCall, registry);
    }
}
