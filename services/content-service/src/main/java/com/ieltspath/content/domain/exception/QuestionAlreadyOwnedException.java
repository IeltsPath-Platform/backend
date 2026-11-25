package com.ieltspath.content.domain.exception;

import java.util.List;
import java.util.UUID;

/**
 * A question has one home. One that already belongs to another exercise block, or to a package section, cannot be the
 * exercise of this block as well: the lesson could not be published, and a learner would meet the question twice.
 */
public class QuestionAlreadyOwnedException extends ContentDomainException {
    private final List<UUID> questionVersionIds;

    public QuestionAlreadyOwnedException(List<UUID> questionVersionIds) {
        super("Câu hỏi đã thuộc một bài học khác hoặc một gói");
        this.questionVersionIds = List.copyOf(questionVersionIds);
    }

    public List<UUID> getQuestionVersionIds() {
        return questionVersionIds;
    }
}
