package com.ieltspath.content.application.command;

import com.ieltspath.content.domain.vo.ContentStatus;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.Map;
import com.ieltspath.content.domain.vo.Skill;

public record UpdateCourseCommand(UUID id, String name, BigDecimal bandLevel, ContentStatus status,
                                  String coverImageUrl, String description, Map<Skill, Integer> skillUnlockPoints,
                                  Integer comboUnlockPoints, Boolean free, String bannerImageUrl) {
    public UpdateCourseCommand(UUID id, String name, BigDecimal bandLevel, ContentStatus status,
                               String coverImageUrl, String description, Map<Skill, Integer> skillUnlockPoints,
                               Integer comboUnlockPoints, Boolean free) {
        this(id, name, bandLevel, status, coverImageUrl, description, skillUnlockPoints, comboUnlockPoints, free, null);
    }
    public UpdateCourseCommand(UUID id, String name, BigDecimal bandLevel, ContentStatus status,
                               String coverImageUrl, String description, Map<Skill, Integer> skillUnlockPoints,
                               Integer comboUnlockPoints) {
        this(id, name, bandLevel, status, coverImageUrl, description, skillUnlockPoints, comboUnlockPoints, null);
    }
    public UpdateCourseCommand(UUID id, String name, BigDecimal bandLevel, ContentStatus status,
                               String coverImageUrl, String description) {
        this(id, name, bandLevel, status, coverImageUrl, description, null, null, null);
    }
    public UpdateCourseCommand(UUID id, String name, BigDecimal bandLevel, ContentStatus status) {
        this(id, name, bandLevel, status, null, null, null, null, null);
    }
}
