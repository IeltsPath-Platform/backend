package com.ieltspath.content.domain.exception;

/** The band pool of a placement test that has been published cannot change: it would skew learners' band results. */
public class PlacementPoolLockedException extends ContentDomainException {
    public PlacementPoolLockedException() {
        super("Đề đã xuất bản nên không đổi được kho band. Gỡ đề này và tạo đề mới cho kho khác.");
    }
}
