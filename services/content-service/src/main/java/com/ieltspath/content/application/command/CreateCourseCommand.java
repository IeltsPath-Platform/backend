package com.ieltspath.content.application.command;

import java.math.BigDecimal;

public record CreateCourseCommand(String code, String name, BigDecimal bandLevel, String coverImageUrl,
                                   String description) {
    public CreateCourseCommand(String code, String name, BigDecimal bandLevel) {
        this(code, name, bandLevel, null, null);
    }
}
