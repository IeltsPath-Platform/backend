package com.ieltspath.library.application.result;

import com.ieltspath.library.domain.study.StudyUnitRules.Issue;
import java.time.Instant;
import java.util.*;

public final class StudyContentResult {
    private StudyContentResult() {}
    public record Word(UUID vocabularyItemId, UUID vocabularySenseId, int sortOrder, String lemma, String partOfSpeech,
        String vietnameseMeaning, String englishDefinition, String ipaUk, String ipaUs, String audioUkUrl,
        String audioUsUrl, String imageUrl, String exampleSentence, String exampleTranslationVi) {}
    public record UnitSummary(UUID unitId, UUID topicId, int revision, String title, String description, int sortOrder,
        String kind, int wordCount, int estimatedMinutes, List<String> exerciseTypes) {}
    public record TopicUnits(UUID topicId, int curriculumVersion, int totalUnits, int uniqueWordCount, List<UnitSummary> items) {}
    public record UnitContent(UnitSummary unit, String introductionVi, List<Word> words) {}
    public record AuthorWord(UUID senseId, String lemma, String vietnameseMeaning, int sortOrder) {}
    public record Revision(int revision, String title, String description, String introductionVi, List<AuthorWord> words) {}
    public record AuthorUnit(UUID unitId, UUID topicId, int sortOrder, String kind, String status, Integer publishedRevision,
        Integer draftRevision, Revision revision, Instant updatedAt) {}
    public record Preview(int wordCount, int gradedStepCount, int fillStepCount, List<String> exerciseTypes) {}
    public record Validation(UUID unitId, int revision, boolean canPublish, List<Issue> errors, List<Issue> warnings, Preview preview) {}
}
