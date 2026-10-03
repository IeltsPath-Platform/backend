package com.group01.learning.application.service;

import com.group01.learning.application.command.SubmitExerciseCommand;
import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.LearningContentClient.Item;
import com.group01.learning.application.port.LearningContentClient.PackageVersion;
import com.group01.learning.application.port.LearningContentClient.Section;
import com.group01.learning.domain.service.AnswerSpecGrader;
import com.group01.learning.domain.service.PassMark;

import java.util.Comparator;
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
        return new Graded(items, grades, correct, items.size(), PassMark.passes(correct, items.size()));
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

    public record Graded(List<Item> items, List<AnswerSpecGrader.Grade> grades,
                         int correct, int total, boolean passed) {
        public double percent() { return (double) correct / total; }
    }
}
