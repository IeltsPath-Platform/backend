package com.ieltspath.learning.domain.vo;

/**
 * {@code PASSED}: the topic test scored 70% or more (or the topic has no test and its lessons are done).
 * {@code COMPLETED}: the topic test was taken below 70%; the topic counts as done and the next one is open.
 */
public enum TopicStatus {
    PASSED,
    COMPLETED,
    IN_PROGRESS,
    LOCKED
}
