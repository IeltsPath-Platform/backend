package com.ieltspath.learning.api.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.result.ReviewSubmissionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewSubmissionResponseTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void oldStoredResponsesWithoutBlockingStillDeserialize() throws Exception {
        ReviewSubmissionResult stored = json.readValue("""
                {"reviewStatus":"PENDING","setPassed":false,"results":[],"transcript":null,
                 "stage":"PRACTICE","failedSets":2,"gamification":null}
                """, ReviewSubmissionResult.class);

        assertThat(stored.blocking()).isNull();
    }

    @Test
    void submissionResponseIncludesWhetherTheReviewStillBlocks() {
        var result = new ReviewSubmissionResult("PENDING", false, List.of(), null, "PRACTICE", 4,
                Boolean.FALSE, null);

        assertThat(ReviewSubmissionResponse.from(result).blocking()).isFalse();
    }
}
