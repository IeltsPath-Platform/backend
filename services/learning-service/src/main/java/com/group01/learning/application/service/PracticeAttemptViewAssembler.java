package com.group01.learning.application.service;

import com.group01.learning.application.port.LearningContentClient.PackageVersion;
import com.group01.learning.application.result.LessonResult;
import com.group01.learning.application.result.PracticeAttemptView;
import com.group01.learning.domain.aggregate.PracticeAttempt;
import org.springframework.stereotype.Component;

@Component
public class PracticeAttemptViewAssembler {
    public PracticeAttemptView assemble(PracticeAttempt attempt, PackageVersion version) {
        var first = ItemGrading.orderedSections(version).stream().findFirst().orElse(null);
        PracticeAttemptView.Audio audio = first == null || first.audio() == null ? null
                : new PracticeAttemptView.Audio(first.audio().mediaUrl(), first.audio().durationSeconds());
        var questions = ItemGrading.orderedItems(version).stream().map(item -> new LessonResult.Question(
                item.questionVersionId(), item.sortOrder(), item.stem(), item.options() == null ? null
                : item.options().stream().map(option -> new LessonResult.Option(option.optionKey(), option.content(),
                        option.sortOrder())).toList(), item.hint())).toList();
        return new PracticeAttemptView(attempt.id(), attempt.packageId(), attempt.packageVersionId(),
                first == null ? null : first.passage(), audio, questions);
    }
}
