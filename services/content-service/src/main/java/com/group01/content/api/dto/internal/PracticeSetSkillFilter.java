package com.group01.content.api.dto.internal;

import com.group01.content.domain.vo.Skill;

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
            throw new IllegalArgumentException("skill must be LISTENING, READING, WRITING or SPEAKING");
        }
        if (skill == Skill.ALL) throw new IllegalArgumentException("skill must select one skill");
        return Optional.of(skill);
    }
}
