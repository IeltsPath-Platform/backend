package com.group01.user.api.exception;

import com.group01.user.domain.exception.InvalidDataException;
import com.group01.user.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LearningSupportErrorResponseTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void activityErrorsKeepTheExistingResponseBody() {
        var request = new MockHttpServletRequest("GET", "/api/learning-support/activities");

        var invalid = handler.handleInvalidActivityData(new InvalidDataException(), request);
        assertEquals(400, invalid.getStatusCode().value());
        assertEquals("Bad Request", invalid.getBody().error());
        assertEquals("Dữ liệu không hợp lệ", invalid.getBody().message());

        var missing = handler.handleNotFound(new ResourceNotFoundException(), request);
        assertEquals(404, missing.getStatusCode().value());
        assertEquals("Not Found", missing.getBody().error());
        assertEquals("Không tìm thấy", missing.getBody().message());
    }

    @Test
    void existingUserErrorFormatIsPreserved() {
        var request = new MockHttpServletRequest("GET", "/api/users");

        var response = handler.handleBadRequest(new IllegalArgumentException("invalid user request"), request);
        assertEquals(400, response.getStatusCode().value());
        assertEquals("Yêu cầu không hợp lệ", response.getBody().error());
        assertEquals("invalid user request", response.getBody().message());
    }
}
