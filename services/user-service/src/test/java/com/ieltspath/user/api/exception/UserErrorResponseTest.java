package com.ieltspath.user.api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserErrorResponseTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void existingUserErrorFormatIsPreserved() {
        var request = new MockHttpServletRequest("GET", "/api/users");

        var response = handler.handleBadRequest(new IllegalArgumentException("invalid user request"), request);
        assertEquals(400, response.getStatusCode().value());
        assertEquals("Yêu cầu không hợp lệ", response.getBody().error());
        assertEquals("invalid user request", response.getBody().message());
    }
}
