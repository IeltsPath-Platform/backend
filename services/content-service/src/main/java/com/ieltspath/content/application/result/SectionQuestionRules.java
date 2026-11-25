package com.ieltspath.content.application.result;

/** Runtime limits and validation thresholds for section question generation. */
public record SectionQuestionRules(
        int minSourceWords,
        int maxSourceChars,
        int maxCount,
        double duplicateThreshold,
        double sameAnswerThreshold,
        int maxAnswerWords,
        int maxCopiedWords,
        int maxStemChars,
        int dailyLimit
) {}
