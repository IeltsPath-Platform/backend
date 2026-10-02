package com.group01.learning.application.usecase;

import com.group01.learning.application.command.SubmitExerciseCommand;
import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.ExerciseSubmissionLog;
import com.group01.learning.application.port.LearnerLock;
import com.group01.learning.application.port.LearningContentClient.Block;
import com.group01.learning.application.port.LearningContentClient.Lesson;
import com.group01.learning.application.port.LearningContentClient.Question;
import com.group01.learning.application.result.SubmissionResult;
import com.group01.learning.application.service.AnswerSheet;
import com.group01.learning.application.service.LessonAccess;
import com.group01.learning.application.service.LessonEvidenceReference;
import com.group01.learning.application.service.LessonViewAssembler;
import com.group01.learning.application.service.ReviewReevaluation;
import com.group01.learning.domain.aggregate.LessonProgress;
import com.group01.learning.domain.repository.KnowledgeEvidenceRepository;
import com.group01.learning.domain.repository.LessonProgressRepository;
import com.group01.learning.domain.service.AnswerSpecGrader;
import com.group01.learning.domain.service.PassMark;
import com.group01.learning.domain.vo.EvidenceSource;
import com.group01.learning.domain.vo.KnowledgeEvidence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Grades one exercise block. A {@code requestId} replays its saved response. Only the first submission of a block is
 * mastery evidence; a passed block reveals its solutions, and completing the lesson may insert reviews.
 */
@Service
public class SubmitLessonExerciseUseCase {
    private static final Logger log = LoggerFactory.getLogger(SubmitLessonExerciseUseCase.class);

    private final LearnerLock lock;
    private final LessonAccess access;
    private final ExerciseSubmissionLog submissions;
    private final KnowledgeEvidenceRepository evidence;
    private final LessonProgressRepository lessons;
    private final ReviewReevaluation reviews;
    private final LessonViewAssembler view;
    private final AnswerSpecGrader grader = new AnswerSpecGrader();
    private final Clock clock = Clock.systemUTC();

    public SubmitLessonExerciseUseCase(LearnerLock lock, LessonAccess access, ExerciseSubmissionLog submissions,
                                       KnowledgeEvidenceRepository evidence, LessonProgressRepository lessons,
                                       ReviewReevaluation reviews, LessonViewAssembler view) {
        this.lock = lock;
        this.access = access;
        this.submissions = submissions;
        this.evidence = evidence;
        this.lessons = lessons;
        this.reviews = reviews;
        this.view = view;
    }

    @Transactional
    public SubmissionResult execute(UUID userId, UUID lessonId, UUID blockId, SubmitExerciseCommand command) {
        lock.lock(userId);
        var saved = submissions.find(command.requestId());
        if (saved.isPresent()) {
            var submission = saved.get();
            if (!submission.userId().equals(userId) || !submission.lessonId().equals(lessonId)
                    || !submission.blockId().equals(blockId)) throw requestConflict();
            return submission.response();
        }
        LessonAccess.Context context = access.authorize(userId, lessonId);
        Lesson lesson = context.lesson();
        Block block = lesson.blocks().stream().filter(item -> item.blockId().equals(blockId)
                        && "EXERCISE".equals(item.blockType())).findFirst()
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Exercise block was not found"));
        if (block.isEssay()) {
            throw new LearningRequestException(409, "ESSAY_BLOCK", "Essay blocks are submitted as essays");
        }
        List<Question> questions = LessonViewAssembler.orderedQuestions(block);
        Map<UUID, Object> answers = AnswerSheet.require(
                questions.stream().map(Question::questionVersionId).toList(), command.answers());
        Set<UUID> opened = new HashSet<>(submissions.wrongQuestions(userId, lessonId)
                .getOrDefault(blockId, Set.of()));
        List<AnswerSpecGrader.Grade> grades = questions.stream()
                .map(question -> grader.grade(question.answerSpec(), answers.get(question.questionVersionId()))).toList();
        if (grades.stream().anyMatch(grade -> !grade.gradable())) {
            throw new LearningRequestException(422, "UNGRADABLE_EXERCISE", "Exercise contains unsupported questions");
        }
        boolean firstSubmission = !submissions.exists(userId, lessonId, blockId);
        Set<UUID> knownKps = firstSubmission ? access.knownKnowledgePoints(userId, exerciseKnowledgePoints(lesson))
                : Set.of();
        // Refresh may change the first unpassed topic; authorize again before changing progress.
        if (firstSubmission) access.reauthorize(userId, context);
        LessonProgress progress = access.refresh(userId, lesson, context.progress());
        boolean blockPassed = PassMark.passes(
                grades.stream().filter(AnswerSpecGrader.Grade::correct).count(), questions.size());
        if (blockPassed) progress.passBlock(blockId);
        Set<UUID> passed = new HashSet<>(progress.passedBlockIds());
        if (firstSubmission) {
            List<KnowledgeEvidence> firstAnswers = new ArrayList<>();
            for (int index = 0; index < questions.size(); index++) {
                Question question = questions.get(index);
                for (UUID kpId : new LinkedHashSet<>(question.knowledgePointIds())) {
                    if (knownKps.contains(kpId)) {
                        firstAnswers.add(KnowledgeEvidence.of(kpId, grades.get(index).correct(),
                                EvidenceSource.LESSON_EXERCISE,
                                LessonEvidenceReference.create(command.requestId(), question.questionVersionId(), kpId)));
                    } else {
                        log.warn("Unknown lesson evidence KP: userId={}, questionVersionId={}, kpId={}",
                                userId, question.questionVersionId(), kpId);
                    }
                }
            }
            evidence.append(userId, firstAnswers);
        }
        // Essay blocks never gate completion: a learner without points can still finish every lesson.
        boolean lessonCompleted = LessonAccess.completed(context.progress()) || lesson.blocks().stream()
                .filter(Block::isExercise).allMatch(item -> passed.contains(item.blockId()));
        List<SubmissionResult.AnswerResult> results = new ArrayList<>();
        for (int index = 0; index < questions.size(); index++) {
            var question = questions.get(index);
            var grade = grades.get(index);
            if (!grade.correct()) opened.add(question.questionVersionId());
            results.add(new SubmissionResult.AnswerResult(question.questionVersionId(), grade.correct(),
                    blockPassed ? grade.correctAnswer() : null, blockPassed ? question.explanation() : null,
                    view.hint(question, passed.contains(blockId), opened)));
        }
        SubmissionResult response = new SubmissionResult(blockPassed, lessonCompleted, List.copyOf(results));
        if (!submissions.save(userId, lessonId, blockId, command, response)) throw requestConflict();
        boolean newlyCompleted = lessonCompleted && progress.complete(clock.instant());
        lessons.save(progress);
        if (newlyCompleted) reevaluate(userId, lesson);
        return response;
    }

    private static Set<UUID> exerciseKnowledgePoints(Lesson lesson) {
        return lesson.blocks().stream().filter(Block::isExercise)
                .flatMap(block -> block.questions().stream()).flatMap(question -> question.knowledgePointIds().stream())
                .collect(Collectors.toSet());
    }

    /** Knowledge points answered wrong on the first try of any block are the ones a review may target. */
    private void reevaluate(UUID userId, Lesson lesson) {
        Map<UUID, List<UUID>> kpsByQuestion = lesson.blocks().stream()
                .filter(Block::isExercise).flatMap(block -> block.questions().stream())
                .collect(Collectors.toMap(Question::questionVersionId, Question::knowledgePointIds, (first, second) -> first));
        Set<UUID> wrong = submissions.firstResponses(userId, lesson.lessonId()).stream()
                .flatMap(submission -> submission.results().stream()).filter(result -> !result.correct())
                .flatMap(result -> kpsByQuestion.getOrDefault(result.questionVersionId(), List.of()).stream())
                .collect(Collectors.toSet());
        reviews.execute(userId, new HashSet<>(lesson.knowledgePointIds()), wrong);
    }

    private static LearningRequestException requestConflict() {
        return new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");
    }
}
