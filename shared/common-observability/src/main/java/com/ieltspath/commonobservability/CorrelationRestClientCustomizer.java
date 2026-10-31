package com.ieltspath.commonobservability;

import com.ieltspath.commonsecurity.header.SecurityHeaders;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.web.client.RestClient;

/** Adds the current request's correlation id to every outgoing REST call made through the auto-configured builder. */
public class CorrelationRestClientCustomizer implements RestClientCustomizer {
    @Override
    public void customize(RestClient.Builder builder) {
        builder.requestInterceptor((request, body, execution) -> {
            String id = MDC.get(CorrelationIdFilter.MDC_KEY);
            if (id != null && !request.getHeaders().containsKey(SecurityHeaders.CORRELATION_ID)) {
                request.getHeaders().set(SecurityHeaders.CORRELATION_ID, id);
            }
            return execution.execute(request, body);
        });
    }
}
