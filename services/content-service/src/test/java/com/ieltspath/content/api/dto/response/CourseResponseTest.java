package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.result.CourseResult;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.MediaReferencePolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourseResponseTest {
    @Test
    void resolvesImageKeysAndKeepsLegacyUrls() {
        CourseResult keyed = result("content-image/u/cover.png");
        CourseResult legacy = result("https://images.example/legacy.jpg");

        assertThat(CourseResponse.from(keyed, new MediaReferencePolicy("https://cdn.example/media"))
                .coverImageUrl()).isEqualTo("https://cdn.example/media/content-image/u/cover.png");
        assertThat(CourseResponse.from(legacy, new MediaReferencePolicy(""))
                .coverImageUrl()).isEqualTo("https://images.example/legacy.jpg");
    }

    @Test
    void leavesUnresolvableImageKeyNullWhenMediaBaseIsBlank() {
        assertThat(CourseResponse.from(result("content-image/u/cover.png"), new MediaReferencePolicy(""))
                .coverImageUrl()).isNull();
    }

    private static CourseResult result(String reference) {
        Instant now = Instant.now();
        return new CourseResult(UUID.randomUUID(), "IELTS", "IELTS", new BigDecimal("5.5"),
                ContentStatus.ACTIVE, now, now, reference, null, Map.of(), null);
    }
}
