package com.ieltspath.learning.application.port;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The review is read from Assessment and handed to the learner as it is, so the JSON must survive the round trip. */
class HumanReviewClientJsonTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void readsTheMarkedErrorsAssessmentReturnsAndKeepsAnAbsentCodeAbsent() throws Exception {
        String assessment = """
                {"reviewSubmissionId":"19d211e9-c99b-4dd7-bf2d-612fc728d5a4","status":"DONE","band":6.5,
                 "criteria":[{"criterion":"GRA","band":6.0,"comment":"Clear."}],
                 "errorCodes":["LINKER_MISUSE"],"examinerId":"7d1b4c5e-1a2b-4c3d-8e9f-0a1b2c3d4e5f","feedback":"Develop it.",
                 "annotations":[
                   {"excerpt":"However, the","start":120,"end":132,"errorCode":"LINKER_MISUSE","suggestion":"Use In addition."},
                   {"excerpt":"have rose","start":812,"end":821,"suggestion":"have risen"}]}
                """;
        HumanReviewClient.Review review = json.readValue(assessment, HumanReviewClient.Review.class);

        assertEquals(java.util.UUID.fromString("7d1b4c5e-1a2b-4c3d-8e9f-0a1b2c3d4e5f"), review.examinerId());
        assertEquals(2, review.annotations().size());
        assertEquals("LINKER_MISUSE", review.annotations().get(0).errorCode());
        assertNull(review.annotations().get(1).errorCode());
        assertEquals(812, review.annotations().get(1).start());

        JsonNode handedOn = json.readTree(json.writeValueAsString(review));
        assertTrue(handedOn.path("annotations").get(0).has("errorCode"));
        assertFalse(handedOn.path("annotations").get(1).has("errorCode"));
        assertEquals("have risen", handedOn.path("annotations").get(1).path("suggestion").asText());
    }

    @Test
    void aReviewWithoutMarkedErrorsHasNoAnnotations() throws Exception {
        HumanReviewClient.Review review = json.readValue("""
                {"reviewSubmissionId":"19d211e9-c99b-4dd7-bf2d-612fc728d5a4","status":"PENDING"}
                """, HumanReviewClient.Review.class);
        assertNull(review.annotations());
    }
}
