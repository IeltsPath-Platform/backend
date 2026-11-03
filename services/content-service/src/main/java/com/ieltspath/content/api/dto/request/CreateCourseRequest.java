package com.ieltspath.content.api.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Map;
import com.ieltspath.content.domain.vo.Skill;

public record CreateCourseRequest(@NotBlank @Size(max = 100) String code,
                                  @NotBlank @Size(max = 255) String name,
                                  @NotNull @DecimalMin("0.0") @DecimalMax("9.0") BigDecimal bandLevel,
                                  @Size(max = 500) @Pattern(regexp = "(?i)^\\s*(https?://\\S+|content-image/[A-Za-z0-9._/-]+)?\\s*$") String coverImageUrl,
                                  @Size(max = 2000) String description,
                                  Map<Skill, Integer> skillUnlockPoints, Integer comboUnlockPoints,
                                  Boolean free, @Size(max = 500) @Pattern(regexp = "(?i)^\\s*(https?://\\S+|content-image/[A-Za-z0-9._/-]+)?\\s*$") String bannerImageUrl) {
    public CreateCourseRequest(String code, String name, BigDecimal bandLevel, String coverImageUrl,
                               String description, Map<Skill, Integer> skillUnlockPoints, Integer comboUnlockPoints,
                               Boolean free) {
        this(code, name, bandLevel, coverImageUrl, description, skillUnlockPoints, comboUnlockPoints, free, null);
    }
    public CreateCourseRequest(String code, String name, BigDecimal bandLevel, String coverImageUrl,
                               String description, Map<Skill, Integer> skillUnlockPoints, Integer comboUnlockPoints) {
        this(code, name, bandLevel, coverImageUrl, description, skillUnlockPoints, comboUnlockPoints, null);
    }
    public CreateCourseRequest(String code, String name, BigDecimal bandLevel, String coverImageUrl,
                               String description) {
        this(code, name, bandLevel, coverImageUrl, description, Map.of(), null, null);
    }
    public CreateCourseRequest(String code, String name, BigDecimal bandLevel) {
        this(code, name, bandLevel, null, null, Map.of(), null, null);
    }
}
