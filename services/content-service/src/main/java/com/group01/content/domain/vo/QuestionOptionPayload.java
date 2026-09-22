package com.group01.content.domain.vo;

public record QuestionOptionPayload(
        String optionKey,
        String content,
        int sortOrder
) {}

