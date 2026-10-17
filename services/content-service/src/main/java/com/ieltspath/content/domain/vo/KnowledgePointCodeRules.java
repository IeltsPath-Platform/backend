package com.ieltspath.content.domain.vo;

import java.util.regex.Pattern;

public final class KnowledgePointCodeRules {
    private static final Pattern CODE = Pattern.compile("^[RLWGV](55|60|65|70)_[A-Z0-9_]{2,60}$");

    private KnowledgePointCodeRules() {}

    public static boolean isValid(String code, KnowledgePointKind kind, Skill skill) {
        return code != null && CODE.matcher(code).matches() && isPrefixCompatible(code, kind, skill);
    }

    public static boolean isPrefixCompatible(String code, KnowledgePointKind kind, Skill skill) {
        if (code == null || code.isEmpty()) return false;
        char prefix = code.charAt(0);
        if (prefix == 'G') {
            return kind == KnowledgePointKind.GRAMMAR && skill == Skill.ALL;
        }
        if (prefix == 'V') {
            return kind == KnowledgePointKind.VOCABULARY && skill == Skill.ALL;
        }
        return true;
    }
}
