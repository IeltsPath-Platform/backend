package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.result.CodeSetting;

public record CodeSettingResponse(String kind, boolean applied, String pattern, long startNumber, boolean editable) {
    public static CodeSettingResponse from(CodeSetting value) {
        return new CodeSettingResponse(value.kind(), value.applied(), value.pattern(), value.startNumber(), value.editable());
    }
}
