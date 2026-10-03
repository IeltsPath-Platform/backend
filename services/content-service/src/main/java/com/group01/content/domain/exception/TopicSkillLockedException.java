package com.group01.content.domain.exception;

/** The topic already has published lessons, so its skill cannot change. */
public class TopicSkillLockedException extends ContentDomainException {
    public TopicSkillLockedException(String topicCode) {
        super("Topic '" + topicCode + "' has published lessons; its skill cannot change");
    }
}
