package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.result.CodeSuggestion;

public record CodeSuggestionResponse(String code, boolean applied, boolean editable) {
    public static CodeSuggestionResponse from(CodeSuggestion value) {
        return new CodeSuggestionResponse(value.code(), value.applied(), value.editable());
    }
}
