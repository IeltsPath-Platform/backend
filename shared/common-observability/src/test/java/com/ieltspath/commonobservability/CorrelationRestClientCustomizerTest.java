package com.ieltspath.commonobservability;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ieltspath.commonsecurity.header.SecurityHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;

class CorrelationRestClientCustomizerTest {
    private static final String URL = "http://content-service/internal/lessons";

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void outgoingCallCarriesTheCorrelationIdOfTheCurrentRequest() {
        var builder = RestClient.builder();
        new CorrelationRestClientCustomizer().customize(builder);
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL))
                .andExpect(header(SecurityHeaders.CORRELATION_ID, "req-42"))
                .andRespond(withSuccess());
        MDC.put(CorrelationIdFilter.MDC_KEY, "req-42");

        builder.build().get().uri(URL).retrieve().toBodilessEntity();

        server.verify();
    }

    @Test
    void outgoingCallOutsideARequestSendsNoHeader() {
        var builder = RestClient.builder();
        new CorrelationRestClientCustomizer().customize(builder);
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL))
                .andExpect(request -> {
                    if (request.getHeaders().containsKey(SecurityHeaders.CORRELATION_ID)) {
                        throw new AssertionError("unexpected correlation header");
                    }
                })
                .andRespond(withSuccess());

        builder.build().get().uri(URL).retrieve().toBodilessEntity();

        server.verify();
    }
}
