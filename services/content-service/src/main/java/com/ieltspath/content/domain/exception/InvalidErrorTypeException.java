package com.ieltspath.content.domain.exception;

/** An error type or a distractor mapping breaks a rule of the error taxonomy. */
public class InvalidErrorTypeException extends ContentDomainException {
    public static final String TRAP_KNOWLEDGE_POINT_NOT_ASSIGNABLE = "TRAP_KNOWLEDGE_POINT_NOT_ASSIGNABLE";

    private final String code;

    public InvalidErrorTypeException(String message) {
        this(null, message);
    }

    private InvalidErrorTypeException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** A knowledge point reserved for an error trap cannot be put on a question or used as a prerequisite. */
    public static InvalidErrorTypeException trapKnowledgePointNotAssignable() {
        return new InvalidErrorTypeException(TRAP_KNOWLEDGE_POINT_NOT_ASSIGNABLE,
                "Điểm kiến thức dành cho bẫy lỗi không gán được vào câu hỏi hay làm điểm tiên quyết");
    }

    /** The stable code the client can act on, or null when the message is all there is. */
    public String getCode() {
        return code;
    }
}
