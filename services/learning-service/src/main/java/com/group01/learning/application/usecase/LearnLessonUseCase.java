package com.group01.learning.application.usecase;

import com.group01.learning.application.LessonEvidenceReference;
import com.group01.learning.application.ReviewReevaluation;
import com.group01.learning.application.command.SubmitExerciseCommand;
import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.port.LearningContentClient.*;
import com.group01.learning.application.port.LearningProgressStore;
import com.group01.learning.application.port.LearningProgressStore.NewEvidence;
import com.group01.learning.application.result.LessonResult;
import com.group01.learning.application.result.SubmissionResult;
import com.group01.learning.application.result.TopicLessonsResult;
import com.group01.learning.domain.exception.LearningGateException;
import com.group01.learning.domain.service.AnswerSpecGrader;
import com.group01.learning.domain.service.LessonAccessGate;
import com.group01.learning.domain.service.TopicStatusDeriver;
import com.group01.learning.domain.vo.LessonProgress;
import com.group01.learning.domain.vo.TopicStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LearnLessonUseCase {
    private static final Logger log = LoggerFactory.getLogger(LearnLessonUseCase.class);
    private final LearningContentClient content;
    private final LearningProgressStore store;
    private final RefreshLearningTopicsUseCase refreshTopics;
    private final ReviewReevaluation reviews;
    private final AnswerSpecGrader grader = new AnswerSpecGrader();
    private final LessonAccessGate gate = new LessonAccessGate();
    private final TopicStatusDeriver topicStatuses = new TopicStatusDeriver();

    public LearnLessonUseCase(LearningContentClient content, LearningProgressStore store,
                              RefreshLearningTopicsUseCase refreshTopics, ReviewReevaluation reviews) {
        this.content = content;
        this.store = store;
        this.refreshTopics = refreshTopics;
        this.reviews = reviews;
    }

    @Transactional(readOnly = true)
    public TopicLessonsResult list(UUID userId, UUID topicId) {
        TopicStatus topicStatus = status(userId, topicId);
        if (topicStatus == TopicStatus.LOCKED) throw new LearningGateException("TOPIC_LOCKED", List.of());
        var lessons = orderedLessons(content.getTopicLessons(topicId));
        var progress = store.findLessons(userId, topicId);
        boolean previousComplete = true;
        List<TopicLessonsResult.LessonSummary> summaries = new ArrayList<>();
        for (var lesson : lessons) {
            boolean completed = completed(progress.get(lesson.lessonId()));
            summaries.add(new TopicLessonsResult.LessonSummary(lesson.lessonId(), lesson.code(), lesson.title(),
                    lesson.sortOrder(), completed ? "COMPLETED" : previousComplete ? "AVAILABLE" : "LOCKED"));
            previousComplete &= completed;
        }
        boolean hasPendingReview = !store.findPendingReviews(userId).isEmpty();
        String testStatus = topicStatus == TopicStatus.PASSED ? "PASSED"
                : previousComplete && !hasPendingReview ? "AVAILABLE" : "LOCKED";
        return new TopicLessonsResult(topicId, List.copyOf(summaries), testStatus);
    }

    @Transactional
    public LessonResult get(UUID userId, UUID lessonId) {
        store.lockUser(userId);
        LessonContext context = authorize(userId, lessonId);
        refreshMetadata(userId, context.lesson());
        Set<UUID> passed = passedBlocks(context.progress());
        List<LessonResult.Block> blocks = orderedBlocks(context.lesson()).stream()
                .map(block -> blockResult(block, passed.contains(block.blockId()))).toList();
        Lesson lesson = context.lesson();
        return new LessonResult(lesson.lessonId(), lesson.topicId(), lesson.code(), lesson.title(), lesson.summary(),
                lesson.sortOrder(), completed(context.progress()) ? "COMPLETED" : "AVAILABLE", blocks);
    }

    @Transactional
    public SubmissionResult submit(UUID userId, UUID lessonId, UUID blockId, SubmitExerciseCommand command) {
        store.lockUser(userId);
        var saved = store.findSubmission(command.requestId());
        if (saved.isPresent()) {
            var submission = saved.get();
            if (!submission.userId().equals(userId) || !submission.lessonId().equals(lessonId)
                    || !submission.blockId().equals(blockId)) throw requestConflict();
            return submission.response();
        }
        LessonContext context = authorize(userId, lessonId);
        Lesson lesson = context.lesson();
        Block block = lesson.blocks().stream().filter(item -> item.blockId().equals(blockId)
                        && "EXERCISE".equals(item.blockType())).findFirst()
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Exercise block was not found"));
        List<Question> questions = orderedQuestions(block);
        Map<UUID, Object> answers = validateAnswers(questions, command);
        List<AnswerSpecGrader.Grade> grades = questions.stream()
                .map(question -> grader.grade(question.answerSpec(), answers.get(question.questionVersionId()))).toList();
        if (grades.stream().anyMatch(grade -> !grade.gradable())) {
            throw new LearningRequestException(422, "UNGRADABLE_EXERCISE", "Exercise contains unsupported questions");
        }
        boolean firstSubmission = !store.hasSubmission(userId, lessonId, blockId);
        Set<UUID> knownKps = firstSubmission ? knownKnowledgePoints(userId, lesson) : Set.of();
        // Refresh may change the first unpassed topic; authorize again before changing progress.
        if (firstSubmission) gate.authorize(store.findPendingReviews(userId), null,
                status(userId, lesson.topicId()), context.previousLessonsComplete());
        refreshMetadata(userId, lesson);
        boolean blockPassed = grades.stream().filter(AnswerSpecGrader.Grade::correct).count() * 10L
                >= questions.size() * 7L;
        Set<UUID> passed = new HashSet<>(passedBlocks(context.progress()));
        if (blockPassed) {
            store.passBlock(userId, lessonId, blockId);
            passed.add(blockId);
        }
        if (firstSubmission) {
            List<NewEvidence> evidence = new ArrayList<>();
            for (int index = 0; index < questions.size(); index++) {
                Question question = questions.get(index);
                for (UUID kpId : new LinkedHashSet<>(question.knowledgePointIds())) {
                    if (knownKps.contains(kpId)) {
                        evidence.add(new NewEvidence(kpId, grades.get(index).correct(), "lesson_exercise",
                                LessonEvidenceReference.create(command.requestId(), question.questionVersionId(), kpId)));
                    } else {
                        log.warn("Unknown lesson evidence KP: userId={}, questionVersionId={}, kpId={}",
                                userId, question.questionVersionId(), kpId);
                    }
                }
            }
            store.appendEvidence(userId, evidence);
        }
        boolean lessonCompleted = completed(context.progress()) || lesson.blocks().stream()
                .filter(item -> "EXERCISE".equals(item.blockType())).allMatch(item -> passed.contains(item.blockId()));
        List<SubmissionResult.AnswerResult> results = new ArrayList<>();
        for (int index = 0; index < questions.size(); index++) {
            var question = questions.get(index);
            var grade = grades.get(index);
            results.add(new SubmissionResult.AnswerResult(question.questionVersionId(), grade.correct(),
                    blockPassed ? grade.correctAnswer() : null, blockPassed ? question.explanation() : null));
        }
        SubmissionResult response = new SubmissionResult(blockPassed, lessonCompleted, List.copyOf(results));
        if (!store.saveSubmission(userId, lessonId, blockId, command, response)) throw requestConflict();
        if (lessonCompleted && store.completeLesson(userId, lessonId)) reevaluate(userId, lesson);
        return response;
    }

    @Transactional
    public UUID complete(UUID userId, UUID lessonId) {
        store.lockUser(userId);
        Lesson lesson = authorize(userId, lessonId).lesson();
        if (lesson.blocks().stream().anyMatch(block -> "EXERCISE".equals(block.blockType()))) {
            throw new LearningRequestException(409, "LESSON_HAS_EXERCISES", "Lesson contains exercise blocks");
        }
        refreshMetadata(userId, lesson);
        if (store.completeLesson(userId, lessonId)) reviews.execute(userId,
                new HashSet<>(lesson.knowledgePointIds()), Set.of());
        return lessonId;
    }

    private LessonContext authorize(UUID userId, UUID lessonId) {
        Lesson lesson = content.getLesson(lessonId);
        var summaries = orderedLessons(content.getTopicLessons(lesson.topicId()));
        var progress = store.findLessons(userId, lesson.topicId());
        boolean previousComplete = true;
        boolean found = false;
        for (var summary : summaries) {
            if (summary.lessonId().equals(lessonId)) { found = true; break; }
            previousComplete &= completed(progress.get(summary.lessonId()));
        }
        gate.authorize(store.findPendingReviews(userId), null, status(userId, lesson.topicId()), previousComplete);
        if (!found) throw new LearningRequestException(404, "NOT_FOUND", "Lesson was not found");
        return new LessonContext(lesson, progress.get(lessonId), previousComplete);
    }

    private TopicStatus status(UUID userId, UUID topicId) {
        return topicStatuses.derive(store.findTopics(userId)).getOrDefault(topicId, TopicStatus.LOCKED);
    }

    private void refreshMetadata(UUID userId, Lesson lesson) {
        store.refreshLesson(userId, lesson.lessonId(), lesson.topicId(), lesson.sortOrder(), lesson.knowledgePointIds());
    }

    private Set<UUID> knownKnowledgePoints(UUID userId, Lesson lesson) {
        Set<UUID> known = store.findMastery(userId).stream().map(history -> history.knowledgePointId())
                .collect(Collectors.toSet());
        Set<UUID> needed = lesson.blocks().stream().filter(block -> "EXERCISE".equals(block.blockType()))
                .flatMap(block -> block.questions().stream()).flatMap(question -> question.knowledgePointIds().stream())
                .collect(Collectors.toSet());
        if (!known.containsAll(needed)) {
            refreshTopics.execute(userId);
            known = store.findMastery(userId).stream().map(history -> history.knowledgePointId()).collect(Collectors.toSet());
        }
        return known;
    }

    private void reevaluate(UUID userId, Lesson lesson) {
        Map<UUID, List<UUID>> kpsByQuestion = lesson.blocks().stream()
                .filter(block -> "EXERCISE".equals(block.blockType())).flatMap(block -> block.questions().stream())
                .collect(Collectors.toMap(Question::questionVersionId, Question::knowledgePointIds, (first, second) -> first));
        Set<UUID> wrong = store.findFirstSubmissions(userId, lesson.lessonId()).stream()
                .flatMap(submission -> submission.results().stream()).filter(result -> !result.correct())
                .flatMap(result -> kpsByQuestion.getOrDefault(result.questionVersionId(), List.of()).stream())
                .collect(Collectors.toSet());
        reviews.execute(userId, new HashSet<>(lesson.knowledgePointIds()), wrong);
    }

    private Map<UUID, Object> validateAnswers(List<Question> questions, SubmitExerciseCommand command) {
        Map<UUID, Object> answers = new HashMap<>();
        Set<UUID> ids = new HashSet<>();
        for (var answer : command.answers()) {
            if (!ids.add(answer.questionVersionId())) throw invalidAnswers();
            answers.put(answer.questionVersionId(), answer.answer());
        }
        Set<UUID> required = questions.stream().map(Question::questionVersionId).collect(Collectors.toSet());
        if (questions.isEmpty() || !ids.equals(required)) throw invalidAnswers();
        return answers;
    }

    private LearningRequestException invalidAnswers() {
        return new LearningRequestException(422, "INVALID_ANSWERS", "Submit one answer for every exercise question");
    }

    private LearningRequestException requestConflict() {
        return new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");
    }

    private boolean completed(LessonProgress progress) { return progress != null && progress.completedAt() != null; }
    private Set<UUID> passedBlocks(LessonProgress progress) { return progress == null ? Set.of() : progress.passedBlockIds(); }
    private List<LessonSummary> orderedLessons(List<LessonSummary> lessons) {
        return lessons.stream().sorted(Comparator.comparingInt(LessonSummary::sortOrder)
                .thenComparing(lesson -> lesson.lessonId().toString())).toList();
    }
    private List<Block> orderedBlocks(Lesson lesson) {
        return lesson.blocks().stream().sorted(Comparator.comparingInt(Block::sortOrder)
                .thenComparing(block -> block.blockId().toString())).toList();
    }
    private List<Question> orderedQuestions(Block block) {
        return block.questions().stream().sorted(Comparator.comparingInt(Question::sortOrder)
                .thenComparing(question -> question.questionVersionId().toString())).toList();
    }

    private LessonResult.Block blockResult(Block block, boolean passed) {
        var asset = block.asset() == null ? null : new LessonResult.Asset(block.asset().id(), block.asset().assetType(),
                block.asset().textContent(), block.asset().mediaReference(), block.asset().durationSeconds());
        boolean exercise = "EXERCISE".equals(block.blockType());
        var questions = exercise ? orderedQuestions(block).stream().map(question -> new LessonResult.Question(
                question.questionVersionId(), question.sortOrder(), question.stem(), question.options() == null ? null
                : question.options().stream().map(option -> new LessonResult.Option(option.optionKey(), option.content(),
                        option.sortOrder())).toList())).toList() : null;
        var solutions = exercise && passed ? orderedQuestions(block).stream().map(question -> new LessonResult.Solution(
                question.questionVersionId(), grader.grade(question.answerSpec(), null).correctAnswer(),
                question.explanation())).toList() : null;
        return new LessonResult.Block(block.blockId(), block.blockType(), block.sortOrder(), block.textContent(), asset,
                block.vocabularySenseIds(), exercise ? passed : null, questions, solutions);
    }

    private record LessonContext(Lesson lesson, LessonProgress progress, boolean previousLessonsComplete) {}
}
