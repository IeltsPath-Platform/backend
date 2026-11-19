package com.ieltspath.content.application.command;

import java.math.BigDecimal;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.Skill;
import java.util.Map;

/** A null {@code status} creates the course active. */
public record CreateCourseCommand(String code, String name, BigDecimal bandLevel, String coverImageUrl,
                                   String description, Map<Skill, Integer> skillUnlockPoints,
                                   Integer comboUnlockPoints, boolean free, String bannerImageUrl,
                                   ContentStatus status) {
    public CreateCourseCommand(String code, String name, BigDecimal bandLevel, String coverImageUrl,
                               String description, Map<Skill, Integer> skillUnlockPoints, Integer comboUnlockPoints,
                               boolean free, String bannerImageUrl) {
        this(code, name, bandLevel, coverImageUrl, description, skillUnlockPoints, comboUnlockPoints, free, bannerImageUrl, null);
    }
    public CreateCourseCommand(String code, String name, BigDecimal bandLevel, String coverImageUrl,
                               String description, Map<Skill, Integer> skillUnlockPoints, Integer comboUnlockPoints,
                               boolean free) {
        this(code, name, bandLevel, coverImageUrl, description, skillUnlockPoints, comboUnlockPoints, free, null);
    }
    public CreateCourseCommand(String code, String name, BigDecimal bandLevel, String coverImageUrl,
                               String description, Map<Skill, Integer> skillUnlockPoints, Integer comboUnlockPoints) {
        this(code, name, bandLevel, coverImageUrl, description, skillUnlockPoints, comboUnlockPoints, false);
    }
    public CreateCourseCommand(String code, String name, BigDecimal bandLevel, String coverImageUrl,
                               String description) {
        this(code, name, bandLevel, coverImageUrl, description, Map.of(), null, false);
    }
    public CreateCourseCommand(String code, String name, BigDecimal bandLevel) {
        this(code, name, bandLevel, null, null, Map.of(), null, false);
    }
}
