package com.ieltspath.content.domain.vo;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A workbook that passed the row checks, resolved into what an import creates. The {@code structure} holds the course,
 * topics, lessons and blocks (question references are still the workbook's {@code importRef} values); the questions and
 * packages are listed separately because the import creates them with their own use cases.
 */
public record CourseImportPlan(CourseDraftStructure structure, List<QuestionPlan> questions,
                               List<PackagePlan> packages, Map<String, UUID> knowledgePointIdsByCode) {

    public CourseImportPlan {
        questions = List.copyOf(questions);
        packages = List.copyOf(packages);
        knowledgePointIdsByCode = Map.copyOf(knowledgePointIdsByCode);
    }

    /** {@code correct} and {@code distractors} belong to choice questions, {@code accepted} to typed ones. */
    public record QuestionPlan(String importRef, QuestionType type, Skill skill, String stem, List<Option> options,
                               String correct, List<String> accepted, Map<String, String> distractors,
                               String explanation, QuestionDifficulty difficulty, List<UUID> knowledgePointIds,
                               int row) {
        public QuestionPlan {
            options = List.copyOf(options);
            accepted = List.copyOf(accepted);
            distractors = Map.copyOf(distractors);
            knowledgePointIds = List.copyOf(knowledgePointIds);
        }

        public boolean choice() {
            return type == QuestionType.MULTIPLE_CHOICE || type == QuestionType.TRUE_FALSE_NOT_GIVEN;
        }
    }

    public record Option(String key, String content) {
    }

    /**
     * A package the import creates. {@code lessonCode} is for practice sets, {@code topicCode} for topic tests and
     * {@code afterTopicSequence} for milestones; {@code questionRefs} are import references in the order listed.
     */
    public record PackagePlan(PackageType type, String code, String title, String lessonCode, String topicCode,
                              Integer afterTopicSequence, List<String> questionRefs, int row) {
        public PackagePlan {
            questionRefs = List.copyOf(questionRefs);
        }
    }
}
