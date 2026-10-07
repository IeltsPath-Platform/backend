package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.ExerciseSubmissionLog;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient.Block;
import com.ieltspath.learning.application.port.LearningContentClient.Lesson;
import com.ieltspath.learning.application.port.LearningContentClient.Question;
import com.ieltspath.learning.application.result.SubmissionResult;
import com.ieltspath.learning.application.service.AnswerSheet;
import com.ieltspath.learning.application.service.LessonAccess;
import com.ieltspath.learning.application.service.LessonEvidenceReference;
import com.ieltspath.learning.application.service.LessonViewAssembler;
import com.ieltspath.learning.application.service.PracticeProgress;
import com.ieltspath.learning.application.service.TopicCompletion;
import com.ieltspath.learning.domain.aggregate.LessonProgress;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.LessonProgressRepository;
import com.ieltspath.learning.domain.service.AnswerSpecGrader;
import com.ieltspath.learning.domain.service.PassMark;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Grades one exercise block. A {@code requestId} replays its saved response. Only the first submission of a block is
 * mastery evidence; a passed block reveals its solutions.
 */
@Service
public class SubmitLessonExerciseUseCase {
    private static final Logger log = LoggerFactory.getLogger(SubmitLessonExerciseUseCase.class);

    private final LearnerLock lock;
    private final LessonAccess access;
    private final ExerciseSubmissionLog submissions;
    private final KnowledgeEvidenceRepository evidence;
    private final LessonProgressRepository lessons;
    private final PracticeProgress practice;
    private final TopicCompletion topicCompletion;
    private final LessonViewAssembler view;
    private final AnswerSpecGrader grader = new AnswerSpecGrader();
    private final Clock clock = Clock.systemUTC();

    public SubmitLessonExerciseUseCase(LearnerLock lock, LessonAccess access, ExerciseSubmissionLog submissions,
                                       KnowledgeEvidenceRepository evidence, LessonProgressRepository lessons,
                                       PracticeProgress practice, LessonViewAssembler view,
                                       TopicCompletion topicCompletion) {
        this.lock = lock;
        this.access = access;
        this.submissions = submissions;
        this.evidence = evidence;
        this.lessons = lessons;
        this.practice = practice;
        this.view = view;
        this.topicCompletion = topicCompletion;
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
        if (newlyCompleted) {
            practice.refreshPassForLesson(userId, lesson.lessonId());
            topicCompletion.onLessonCompleted(userId, lesson.topicId());
        }
        return response;
    }

    private static Set<UUID> exerciseKnowledgePoints(Lesson lesson) {
        return lesson.blocks().stream().filter(Block::isExercise)
                .flatMap(block -> block.questions().stream()).flatMap(question -> question.knowledgePointIds().stream())
                .collect(Collectors.toSet());
    }

    private static LearningRequestException requestConflict() {
        return new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");
    }
}
