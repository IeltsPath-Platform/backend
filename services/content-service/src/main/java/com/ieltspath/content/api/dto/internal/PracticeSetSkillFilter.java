package com.ieltspath.content.api.dto.internal;

import com.ieltspath.content.domain.vo.Skill;

import java.util.Optional;

/** Optional exact single-skill practice filter. */
public final class PracticeSetSkillFilter {
    private PracticeSetSkillFilter() {}

    public static Optional<Skill> parse(String value) {
        if (value == null) return Optional.empty();
        Skill skill;
        try {
            skill = Skill.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Kỹ năng phải là LISTENING, READING, WRITING hoặc SPEAKING");
        }
        if (skill == Skill.ALL) throw new IllegalArgumentException("Chỉ được chọn một kỹ năng");
        return Optional.of(skill);
    }
}
