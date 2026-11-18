package com.ieltspath.assessment.api.exception;

import com.ieltspath.assessment.domain.exception.PlacementLimitReachedException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class PlacementLimitErrorMappingTest {
    @Test
    void mapsTheLimitToTheStableTooManyRequestsContract() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/assessments/attempts");
        var response = new GlobalExceptionHandler().placementLimitReached(new PlacementLimitReachedException(), request);
        assertThat(response.getStatusCode().value()).isEqualTo(429);
        assertThat(response.getBody().code()).isEqualTo("PLACEMENT_LIMIT_REACHED");
    }
}
