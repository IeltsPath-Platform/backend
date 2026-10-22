package com.ieltspath.library.api.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpdateFlashcardRequestJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void whenImageKeyOmittedIsImageKeySpecifiedIsFalse() throws Exception {
        String json = """
                {
                    "sourceType": "MANUAL",
                    "front": "front text",
                    "back": "back text",
                    "status": "ACTIVE"
                }
                """;

        UpdateFlashcardRequest request = mapper.readValue(json, UpdateFlashcardRequest.class);

        assertNotNull(request);
        assertFalse(request.isImageKeySpecified());
        assertNull(request.imageKey());
        assertEquals("front text", request.front());
    }

    @Test
    void whenImageKeyExplicitlyNullIsImageKeySpecifiedIsTrue() throws Exception {
        String json = """
                {
                    "sourceType": "MANUAL",
                    "front": "front text",
                    "back": "back text",
                    "status": "ACTIVE",
                    "imageKey": null
                }
                """;

        UpdateFlashcardRequest request = mapper.readValue(json, UpdateFlashcardRequest.class);

        assertNotNull(request);
        assertTrue(request.isImageKeySpecified());
        assertNull(request.imageKey());
    }

    @Test
    void whenImageKeyProvidedIsImageKeySpecifiedIsTrue() throws Exception {
        String json = """
                {
                    "sourceType": "MANUAL",
                    "front": "front text",
                    "back": "back text",
                    "status": "ACTIVE",
                    "imageKey": "images/user/card.png"
                }
                """;

        UpdateFlashcardRequest request = mapper.readValue(json, UpdateFlashcardRequest.class);

        assertNotNull(request);
        assertTrue(request.isImageKeySpecified());
        assertEquals("images/user/card.png", request.imageKey());
    }

    @Test
    void whenCoverImageKeyOmittedInDeckRequestIsCoverImageKeySpecifiedIsFalse() throws Exception {
        String json = """
                {
                    "name": "My Deck",
                    "description": "My Desc",
                    "status": "ACTIVE"
                }
                """;

        UpdateDeckRequest request = mapper.readValue(json, UpdateDeckRequest.class);

        assertNotNull(request);
        assertFalse(request.isCoverImageKeySpecified());
        assertNull(request.coverImageKey());
    }

    @Test
    void whenCoverImageKeyExplicitlyNullIsCoverImageKeySpecifiedIsTrue() throws Exception {
        String json = """
                {
                    "name": "My Deck",
                    "description": "My Desc",
                    "status": "ACTIVE",
                    "coverImageKey": null
                }
                """;

        UpdateDeckRequest request = mapper.readValue(json, UpdateDeckRequest.class);

        assertNotNull(request);
        assertTrue(request.isCoverImageKeySpecified());
        assertNull(request.coverImageKey());
    }
}

