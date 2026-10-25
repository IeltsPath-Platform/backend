package com.ieltspath.content.domain.service;

import java.util.Map;
import java.util.Set;

/** Maps the two incorrect T/F/NG answer choices that have a defined error code. */
public final class TrueFalseNotGivenTraps {
    private TrueFalseNotGivenTraps() {}

    public static Map<String, String> distractorsFor(String correct, Set<String> optionKeys) {
        if (correct == null || optionKeys == null || optionKeys.isEmpty()) {
            return Map.of();
        }
        if ("NOT_GIVEN".equals(correct)) {
            if (optionKeys.contains("FALSE")) {
                return Map.of("FALSE", "TFNG_NG_AS_FALSE");
            }
            if (optionKeys.contains("NO")) {
                return Map.of("NO", "TFNG_NG_AS_FALSE");
            }
        } else if (("FALSE".equals(correct) || "NO".equals(correct)) && optionKeys.contains("NOT_GIVEN")) {
            return Map.of("NOT_GIVEN", "TFNG_FALSE_AS_NG");
        }
        return Map.of();
    }
}
