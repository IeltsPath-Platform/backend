package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.ExerciseSubmissionLog;
import com.ieltspath.learning.application.port.LearningContentClient.Lesson;
import com.ieltspath.learning.application.port.LearningContentClient.Question;
import com.ieltspath.learning.application.result.SubmissionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Knowledge points the learner answered wrong on the first submission of a lesson's exercise blocks. */
@Component
public class FirstAttemptMistakes {
    private final ExerciseSubmissionLog submissions;

    public FirstAttemptMistakes(ExerciseSubmissionLog submissions) {
        this.submissions = submissions;
    }

    public Set<UUID> of(UUID userId, Lesson lesson) {
        Map<UUID, List<UUID>> kpsByQuestion = new HashMap<>();
        for (var block : lesson.blocks()) {
            if (block.questions() == null) continue;
            for (Question question : block.questions()) {
                kpsByQuestion.put(question.questionVersionId(),
                        question.knowledgePointIds() == null ? List.of() : question.knowledgePointIds());
            }
        }
        Set<UUID> wrong = new HashSet<>();
        for (SubmissionResult response : submissions.firstResponses(userId, lesson.lessonId())) {
            for (var answer : response.results()) {
                if (!answer.correct()) wrong.addAll(kpsByQuestion.getOrDefault(answer.questionVersionId(), List.of()));
            }
        }
        return wrong;
    }
}
