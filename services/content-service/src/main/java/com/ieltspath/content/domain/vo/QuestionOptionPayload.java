package com.ieltspath.content.domain.vo;

public record QuestionOptionPayload(
        String optionKey,
        String content,
        int sortOrder
) {}

