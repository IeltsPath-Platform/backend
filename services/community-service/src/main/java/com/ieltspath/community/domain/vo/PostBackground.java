package com.ieltspath.community.domain.vo;

/**
 * Decorative background for short text posts. The keys are a fixed whitelist; the client decides how each
 * key is painted, so the server stores no colour values.
 */
public enum PostBackground {
    VIOLET, CRIMSON, MIDNIGHT, SUNSET, OCEAN, FOREST, ROSE, AURORA;

    /** Longest body a coloured post may have, so the text stays readable on the background. */
    public static final int MAX_BODY_LENGTH = 280;
}
