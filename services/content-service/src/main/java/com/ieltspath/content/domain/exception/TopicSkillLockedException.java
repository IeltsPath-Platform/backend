package com.ieltspath.content.domain.exception;

/** The topic already has published lessons, so its skill cannot change. */
public class TopicSkillLockedException extends ContentDomainException {
    public TopicSkillLockedException(String topicCode) {
        super("Chủ đề \"" + topicCode + "\" đã có bài học xuất bản nên không đổi kỹ năng được");
    }
}
