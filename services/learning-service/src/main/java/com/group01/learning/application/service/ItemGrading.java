package com.group01.learning.application.service;

import com.group01.learning.application.command.SubmitExerciseCommand;
import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.LearningContentClient.Item;
import com.group01.learning.application.port.LearningContentClient.PackageVersion;
import com.group01.learning.application.port.LearningContentClient.Section;
import com.group01.learning.domain.service.AnswerSpecGrader;
import com.group01.learning.domain.service.PassMark;
import com.group01.learning.domain.vo.LearningSkill;
import com.group01.learning.domain.vo.PracticeSubmission;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ItemGrading {
    private final AnswerSpecGrader grader = new AnswerSpecGrader();

    public Graded grade(PackageVersion version, List<SubmitExerciseCommand.Answer> submitted) {
        return grade(version, submitted, "Practice set");
    }

    public Graded grade(PackageVersion version, List<SubmitExerciseCommand.Answer> submitted, String subject) {
        List<Item> items = orderedItems(version);
        Map<java.util.UUID, Object> answers = AnswerSheet.require(items.stream().map(Item::questionVersionId).toList(),
                submitted);
        List<AnswerSpecGrader.Grade> grades = items.stream()
                .map(item -> grader.grade(item.answerSpec(), answers.get(item.questionVersionId()))).toList();
        if (grades.stream().anyMatch(grade -> !grade.gradable())) {
            throw new LearningRequestException(422, "UNGRADABLE_EXERCISE", subject + " contains unsupported questions");
        }
        int correct = (int) grades.stream().filter(AnswerSpecGrader.Grade::correct).count();
        List<LearningSkill> itemSkills = orderedSections(version).stream().flatMap(section -> {
            LearningSkill skill = skillOf(section);
            return section.items().stream().map(item -> skill);
        }).toList();
        List<PracticeSubmission.SkillScore> scores = skillScores(itemSkills, grades);
        boolean passed = scores.isEmpty() ? PassMark.passes(correct, items.size())
                : scores.stream().allMatch(PracticeSubmission.SkillScore::passed);
        return new Graded(items, grades, correct, items.size(), passed, itemSkills, scores);
    }

    /** One score per skill of the items, in enum order; items of an unknown skill are left out. */
    private static List<PracticeSubmission.SkillScore> skillScores(List<LearningSkill> itemSkills,
                                                                  List<AnswerSpecGrader.Grade> grades) {
        Map<LearningSkill, int[]> counts = new EnumMap<>(LearningSkill.class);
        for (int index = 0; index < itemSkills.size(); index++) {
            if (itemSkills.get(index) == null) continue;
            int[] count = counts.computeIfAbsent(itemSkills.get(index), ignored -> new int[2]);
            if (grades.get(index).correct()) count[0]++;
            count[1]++;
        }
        return counts.entrySet().stream().map(entry -> new PracticeSubmission.SkillScore(entry.getKey(),
                entry.getValue()[0], entry.getValue()[1], (double) entry.getValue()[0] / entry.getValue()[1],
                PassMark.passes(entry.getValue()[0], entry.getValue()[1]))).toList();
    }

    private static LearningSkill skillOf(Section section) {
        if (section.skill() == null) return null;
        try {
            return LearningSkill.valueOf(section.skill());
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    public static List<Section> orderedSections(PackageVersion version) {
        return version.sections().stream().sorted(Comparator.comparingInt(Section::sortOrder)
                .thenComparing(section -> section.sectionId().toString())).toList();
    }

    public static List<Item> orderedItems(PackageVersion version) {
        return orderedSections(version).stream().flatMap(section -> section.items().stream()
                .sorted(Comparator.comparingInt(Item::sortOrder)
                        .thenComparing(item -> item.questionVersionId().toString()))).toList();
    }

    /**
     * {@code itemSkills} is the skill of each item's section, aligned with {@code items}; {@code passed} holds when
     * every skill in {@code skillScores} passes.
     */
    public record Graded(List<Item> items, List<AnswerSpecGrader.Grade> grades,
                         int correct, int total, boolean passed, List<LearningSkill> itemSkills,
                         List<PracticeSubmission.SkillScore> skillScores) {
        public double percent() { return (double) correct / total; }
    }
}
