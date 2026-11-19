package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class KnowledgePointInUseException extends ContentDomainException {
    public KnowledgePointInUseException(UUID id) {
        super("Điểm kiến thức đang được câu hỏi hoặc bài học đã xuất bản sử dụng");
    }
}
