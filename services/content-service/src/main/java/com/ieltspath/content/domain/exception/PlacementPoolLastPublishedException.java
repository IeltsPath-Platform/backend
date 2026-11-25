package com.ieltspath.content.domain.exception;

/** Every placement pool needs at least one published test, so its last one cannot be taken down. */
public class PlacementPoolLastPublishedException extends ContentDomainException {
    public PlacementPoolLastPublishedException() {
        super("Kho đề này chỉ còn một đề đã xuất bản. Thêm đề khác trước khi gỡ đề này.");
    }
}
