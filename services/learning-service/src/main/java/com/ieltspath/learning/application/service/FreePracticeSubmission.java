package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.QuestionEventLog;
import com.ieltspath.learning.domain.aggregate.PracticeAttempt;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.service.LessonHintPolicy;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import com.ieltspath.learning.domain.vo.GamificationSummary;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import com.ieltspath.learning.domain.vo.PracticeSubmission;
import com.ieltspath.learning.domain.vo.QuestionAttemptEvent;
import com.ieltspath.learning.domain.vo.QuestionEventSource;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Grades and records a free-practice attempt. It writes the answers, the question log and mastery evidence, and nothing
 * else: no review is created or promoted, no lesson or course score moves, and the result never blocks anything.
 */
@Component
public class FreePracticeSubmission {
    private final KnowledgeEvidenceRepository evidence;
    private final PracticeAttemptRepository attempts;
    private final QuestionEventLog questionEvents;
    private final MasteryProfileUpdater masteryProfiles;
    private final LearningContentClient content;
    private final ItemGrading grading = new ItemGrading();
    private final LessonHintPolicy hints = new LessonHintPolicy();
    private final Clock clock = Clock.systemUTC();

    private final GamificationRecorder gamificationRecorder;
    private final HeartsService hearts;

    public FreePracticeSubmission(KnowledgeEvidenceRepository evidence, PracticeAttemptRepository attempts,
                                  QuestionEventLog questionEvents, MasteryProfileUpdater masteryProfiles,
                                  LearningContentClient content, GamificationRecorder gamificationRecorder,
                                  HeartsService hearts) {
        this.hearts = hearts;
        this.evidence = evidence;
        this.attempts = attempts;
        this.questionEvents = questionEvents;
        this.masteryProfiles = masteryProfiles;
        this.content = content;
        this.gamificationRecorder = gamificationRecorder;
    }

    /** The caller has checked that the attempt is the learner's, open, and that this request id is new. */
    public PracticeSubmission submit(UUID userId, PracticeAttempt attempt, SubmitExerciseCommand command) {
        var version = content.getPackageVersion(attempt.packageVersionId());
        if (ItemGrading.orderedItems(version).stream().anyMatch(ItemGrading::isEssay)) {
            throw new LearningRequestException(409, "FREE_PRACTICE_ESSAY_UNSUPPORTED",
                    "Free practice does not take essays");
        }
        var graded = grading.grade(version, command.answers(), "Practice set");
        // Only the first answer to a set counts, as in lessons; a repeat is logged but adds no evidence.
        boolean counted = !attempts.revealedPackageIds(userId).contains(attempt.packageId());
        Map<UUID, Long> durations = AnswerDurations.of(command.answers());
        List<KnowledgeEvidence> firstAnswers = new ArrayList<>();
        List<QuestionAttemptEvent> events = new ArrayList<>();
        List<PracticeSubmission.Answer> results = new ArrayList<>();
        for (int index = 0; index < graded.items().size(); index++) {
            var item = graded.items().get(index);
            var grade = graded.grades().get(index);
            Set<UUID> kps = new LinkedHashSet<>();
            for (var mapping : item.knowledgePointMappings()) kps.add(mapping.knowledgePointId());
            if (counted) for (UUID kp : kps) firstAnswers.add(KnowledgeEvidence.of(kp, grade.correct(),
                    EvidenceSource.FREE_PRACTICE,
                    LessonEvidenceReference.forFreePractice(command.requestId(), item.questionVersionId(), kp))
                    .forQuestion(item.questionVersionId()));
            events.add(QuestionAttemptEvent.of(userId, item.questionVersionId(), List.copyOf(kps), grade.correct(),
                    durations.get(item.questionVersionId()), graded.errorCodes().get(index),
                    QuestionEventSource.FREE_PRACTICE, command.requestId(), counted));
            results.add(new PracticeSubmission.Answer(item.questionVersionId(), grade.correct(),
                    grade.correctAnswer(), item.explanation(),
                    hints.afterAnswer(grade.correct(), item.answerSpec(), item.options(), item.hint())));
        }
        GamificationSummary xp = counted
                ? gamificationRecorder.recordCorrectAnswers(userId, command.requestId(), graded.correctQuestionIds())
                : gamificationRecorder.current(userId);
        int wrong = 0;
        for (int index = 0; index < graded.items().size(); index++) {
            if (!graded.grades().get(index).correct()) wrong++;
        }
        HeartsService.Snapshot heartState = hearts.lose(userId, wrong);
        GamificationSummary gamification = xp.withHearts(wrong, heartState.hearts(), heartState.nextHeartAt());
        PracticeSubmission response = new PracticeSubmission(attempt.id(), graded.correct(), graded.total(),
                graded.percent(), graded.passed(), counted, List.copyOf(results), null, List.of(),
                graded.skillScores(), gamification);
        attempt.recordResult(command.requestId(), response, clock.instant());
        attempts.saveResult(attempt);
        questionEvents.append(events);
        evidence.append(userId, firstAnswers);
        masteryProfiles.recordEvidence(userId, firstAnswers);
        return response;
    }
}
