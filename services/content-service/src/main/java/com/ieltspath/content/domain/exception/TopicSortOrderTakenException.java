package com.ieltspath.content.domain.exception;

public class TopicSortOrderTakenException extends ContentDomainException {
    public TopicSortOrderTakenException(int sortOrder) {
        super("Thứ tự " + sortOrder + " đã có chủ đề khác cùng cấp");
    }
}
