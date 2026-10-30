package com.ieltspath.content.application.command;

import java.math.BigDecimal;
import com.ieltspath.content.domain.vo.Skill;
import java.util.Map;

public record CreateCourseCommand(String code, String name, BigDecimal bandLevel, String coverImageUrl,
                                   String description, Map<Skill, Integer> skillUnlockPoints,
                                   Integer comboUnlockPoints) {
    public CreateCourseCommand(String code, String name, BigDecimal bandLevel, String coverImageUrl,
                               String description) {
        this(code, name, bandLevel, coverImageUrl, description, Map.of(), null);
    }
    public CreateCourseCommand(String code, String name, BigDecimal bandLevel) {
        this(code, name, bandLevel, null, null, Map.of(), null);
    }
}
