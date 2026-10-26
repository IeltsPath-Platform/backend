package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.domain.vo.RubricVersionRef;

public record RubricVersionResponse(String code, String task, int version, String status) {
    public static RubricVersionResponse from(RubricVersionRef ref) {
        return new RubricVersionResponse(ref.code(), ref.task(), ref.version(), ref.status());
    }
}
