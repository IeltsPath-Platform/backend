package com.ieltspath.content.application.command;

import java.util.List;
import java.util.Map;

/**
 * Changes to one draft item. A field left null stays as it is; {@code options} and {@code distractors} replace the
 * whole list or map when given, so an empty map clears the error codes.
 */
public record QuestionDraftItemPatch(
        String stem,
        String explanation,
        List<Option> options,
        String correct,
        Map<String, String> distractors,
        List<String> accepted
) {
    public record Option(String optionKey, String content) {
    }
}
