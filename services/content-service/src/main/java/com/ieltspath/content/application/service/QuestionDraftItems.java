package com.ieltspath.content.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionType;

import java.util.Locale;

/**
 * The stored shape of a question draft item and how it relates to the plain question a model writes. A candidate has
 * {@code stem}, {@code explanation}, {@code options}, {@code correct}, {@code distractors} or {@code accepted}; a stored
 * item has the answer spec the approval step hands to the question use case, built here and never copied from a model.
 */
public final class QuestionDraftItems {

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private QuestionDraftItems() {
    }

    /** Lower case with runs of whitespace folded to one space, so two stems that differ only in layout match. */
    public static String normalize(String stem) {
        return stem == null ? "" : stem.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** The stored item of a candidate that passed {@link QuestionDraftValidator}. */
    public static ObjectNode toItem(JsonNode candidate, QuestionType type, QuestionDifficulty difficulty) {
        ObjectNode item = NODES.objectNode();
        item.put("stem", candidate.path("stem").asText().trim());
        item.put("explanation", candidate.path("explanation").asText().trim());
        item.put("difficulty", difficulty.name());
        ArrayNode options = NODES.arrayNode();
        ObjectNode answerSpec = NODES.objectNode();
        if (QuestionDraftValidator.isChoice(type)) {
            int order = 1;
            for (JsonNode option : candidate.path("options")) {
                options.add(NODES.objectNode()
                        .put("optionKey", option.path("optionKey").asText())
                        .put("content", option.path("content").asText())
                        .put("sortOrder", order++));
            }
            answerSpec.put("type", "CHOICE");
            answerSpec.put("correct", candidate.path("correct").asText());
            JsonNode distractors = candidate.path("distractors");
            if (distractors.isObject() && distractors.size() > 0) {
                answerSpec.set("distractors", distractors);
            }
        } else {
            answerSpec.put("type", "FILL");
            answerSpec.set("accepted", candidate.path("accepted"));
        }
        item.set("options", options);
        item.set("answerSpec", answerSpec);
        return item;
    }

    /** A stored item turned back into the candidate form, so an edit can be checked by the same rules. */
    public static ObjectNode toCandidate(JsonNode item) {
        ObjectNode candidate = NODES.objectNode();
        candidate.put("stem", item.path("stem").asText());
        candidate.put("explanation", item.path("explanation").asText());
        candidate.set("options", item.path("options").deepCopy());
        JsonNode answerSpec = item.path("answerSpec");
        if (answerSpec.has("correct")) candidate.set("correct", answerSpec.get("correct"));
        if (answerSpec.has("distractors")) candidate.set("distractors", answerSpec.get("distractors"));
        if (answerSpec.has("accepted")) candidate.set("accepted", answerSpec.get("accepted"));
        return candidate;
    }
}
