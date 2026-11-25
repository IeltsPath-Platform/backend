package com.ieltspath.content.domain.exception;

public class InvalidContentStateException extends ContentDomainException {
    public static final String PLACEMENT_POOL_REQUIRED = "PLACEMENT_POOL_REQUIRED";
    public static final String CODE_LOCKED = "CODE_LOCKED";

    private final String code;

    public InvalidContentStateException(String message) {
        this(null, message);
    }

    private InvalidContentStateException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** A placement test is always built for one pool of placement questions. */
    public static InvalidContentStateException placementPoolRequired() {
        return new InvalidContentStateException(PLACEMENT_POOL_REQUIRED, "Đề xếp lớp cần có nhóm đề (placementPool)");
    }

    public static InvalidContentStateException codeLocked() {
        return new InvalidContentStateException(CODE_LOCKED, "Mã được tạo tự động và không thể chỉnh sửa.");
    }

    /** The stable code the client can act on, or null when the message is all there is. */
    public String getCode() {
        return code;
    }
}
