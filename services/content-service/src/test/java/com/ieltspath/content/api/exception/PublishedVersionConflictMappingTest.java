package com.ieltspath.content.api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class PublishedVersionConflictMappingTest {

    @Test
    void mapsPublishedVersionLockToConflict() {
        var request = new MockHttpServletRequest("PUT", "/api/content/questions/example");
        var exception = new DataIntegrityViolationException("database update failed",
                new IllegalStateException("published_version_content_locked"));

        var response = new GlobalExceptionHandler().handleDataIntegrityViolation(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Không thể chỉnh sửa nội dung của phiên bản đã xuất bản");
        assertThat(response.getBody().details()).containsEntry("code", "PUBLISHED_VERSION_LOCKED");
    }
}
