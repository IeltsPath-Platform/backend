package com.ieltspath.commonobservability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ieltspath.commonsecurity.header.SecurityHeaders;
import jakarta.servlet.FilterChain;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void keepsAnIncomingIdAndEchoesItInTheResponse() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(SecurityHeaders.CORRELATION_ID, "gw-7f3a");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> assertEquals("gw-7f3a", MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertEquals("gw-7f3a", response.getHeader(SecurityHeaders.CORRELATION_ID));
    }

    @Test
    void generatesAnIdWhenTheRequestHasNone() throws Exception {
        var response = new MockHttpServletResponse();
        String[] seen = new String[1];

        filter.doFilter(new MockHttpServletRequest(), response, (req, res) -> seen[0] = MDC.get(CorrelationIdFilter.MDC_KEY));

        assertEquals(UUID.fromString(seen[0]).toString(), seen[0]);
        assertEquals(seen[0], response.getHeader(SecurityHeaders.CORRELATION_ID));
    }

    @Test
    void replacesAnIdThatCouldForgeLogLines() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(SecurityHeaders.CORRELATION_ID, "x\nINFO forged line");
        String[] seen = new String[1];

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> seen[0] = MDC.get(CorrelationIdFilter.MDC_KEY));

        assertNotEquals("x\nINFO forged line", seen[0]);
        UUID.fromString(seen[0]);
    }

    @Test
    void clearsTheMdcEvenWhenTheChainFails() {
        var request = new MockHttpServletRequest();
        FilterChain failing = (req, res) -> {
            throw new IllegalStateException("boom");
        };

        assertThrows(IllegalStateException.class,
                () -> filter.doFilter(request, new MockHttpServletResponse(), failing));

        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
