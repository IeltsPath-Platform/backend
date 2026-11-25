package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class SectionDraftOpenException extends RuntimeException {
    private final UUID draftId;
    public SectionDraftOpenException(UUID draftId) {
        super("Đã có một lượt tạo câu hỏi đang mở cho phần này");
        this.draftId = draftId;
    }
    public UUID draftId() { return draftId; }
}
