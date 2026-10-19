package com.ieltspath.commonstorage;

/**
 * File upload bị từ chối. Thông báo cố định, không chứa nội dung file hay tên file.
 */
public class UploadRejectedException extends IllegalArgumentException {

    /** Lý do từ chối, cố định. */
    public enum Reason {
        EMPTY,
        TOO_LARGE,
        TYPE_NOT_ALLOWED,
        CONTENT_MISMATCH
    }

    private final Reason reason;

    public UploadRejectedException(Reason reason) {
        super("upload rejected: " + reason);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
