package com.ieltspath.assessment.application.port;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/** A chart image of a writing prompt, stored with the review request so the examiner reads the essay against what the learner saw. */
public record PromptImage(String url, String altText) {
    /** The images of a stored prompt snapshot's {@code images} array; an entry without a url is not an image. */
    public static List<PromptImage> listOf(JsonNode images) {
        var list = new ArrayList<PromptImage>();
        if (images == null) return list;
        for (JsonNode image : images) {
            String url = image.path("url").asText(null);
            if (url != null && !url.isBlank()) list.add(new PromptImage(url, image.path("altText").asText(null)));
        }
        return list;
    }
}
