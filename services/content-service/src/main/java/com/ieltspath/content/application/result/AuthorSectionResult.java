package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.entity.ContentSection;
import com.ieltspath.content.domain.entity.SectionQuestion;
import com.ieltspath.content.domain.vo.Skill;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record AuthorSectionResult(
        UUID id,
        String title,
        Skill skill,
        int sortOrder,
        Integer timeLimitSeconds,
        Integer partNo,
        UUID coverAssetId,
        String instructions,
        List<AuthorSectionQuestionResult> questions
) {
    public static AuthorSectionResult of(ContentSection section) {
        List<AuthorSectionQuestionResult> questions = section.getQuestions().stream()
                .sorted(Comparator.comparingInt(SectionQuestion::getSortOrder))
                .map(q -> new AuthorSectionQuestionResult(q.getId(), q.getQuestionVersionId(), q.getSortOrder(),
                        q.getMaxScore()))
                .toList();
        return new AuthorSectionResult(section.getId(), section.getTitle(), section.getSkill(),
                section.getSortOrder(), section.getTimeLimitSeconds(), section.getPartNo(),
                section.getCoverAssetId(), section.getInstructions(), questions);
    }
}
