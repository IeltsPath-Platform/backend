package com.group01.learning.application.usecase;

import com.group01.learning.application.AnswerSheet;
import com.group01.learning.application.LessonAccess;
import com.group01.learning.application.LessonEvidenceReference;
import com.group01.learning.application.ReviewReevaluation;
import com.group01.learning.application.command.SubmitExerciseCommand;
import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.port.LearningContentClient.*;
import com.group01.learning.application.port.LearningProgressStore;
import com.group01.learning.application.port.LearningProgressStore.NewEvidence;
import com.group01.learning.application.port.WritingSubmissionStore;
import com.group01.learning.application.result.LessonResult;
import com.group01.learning.application.result.SubmissionResult;
import com.group01.learning.application.result.TopicLessonsResult;
import com.group01.learning.domain.exception.LearningGateException;
import com.group01.learning.domain.service.AnswerSpecGrader;
import com.group01.learning.domain.vo.LessonProgress;
import com.group01.learning.domain.vo.TopicStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LearnLessonUseCase {
    private static final Logger log = LoggerFactory.getLogger(LearnLessonUseCase.class);
    private final LearningContentClient content;
    private final LearningProgressStore store;
    private final LessonAccess access;
    private final ReviewReevaluation reviews;
    private final WritingSubmissionStore essays;
    private final AnswerSpecGrader grader = new AnswerSpecGrader();

    public LearnLessonUseCase(LearningContentClient content, LearningProgressStore store, LessonAccess access,
                              ReviewReevaluation reviews, WritingSubmissionStore essays) {
        this.content = content;
        this.store = store;
        this.access = access;
        this.reviews = reviews;
        this.essays = essays;
    }

    @Transactional(readOnly = true)
    public TopicLessonsResult list(UUID userId, UUID topicId) {
        TopicStatus topicStatus = access.status(userId, topicId);
        if (topicStatus == TopicStatus.LOCKED) throw new LearningGateException("TOPIC_LOCKED", List.of());
        var lessons = LessonAccess.orderedLessons(content.getTopicLessons(topicId));
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
        LessonAccess.Context context = access.authorize(userId, lessonId);
        access.refresh(userId, context.lesson());
        Set<UUID> passed = passedBlocks(context.progress());
        Map<UUID, WritingSubmissionStore.BlockSummary> essaySummaries = essays.summarizeBlocks(userId,
                context.lesson().blocks().stream().filter(Block::isEssay).map(Block::blockId).toList());
        List<LessonResult.Block> blocks = orderedBlocks(context.lesson()).stream()
                .map(block -> blockResult(block, passed.contains(block.blockId()), completed(context.progress()),
                        essaySummaries.get(block.blockId())))
                .toList();
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
        LessonAccess.Context context = access.authorize(userId, lessonId);
        Lesson lesson = context.lesson();
        Block block = lesson.blocks().stream().filter(item -> item.blockId().equals(blockId)
                        && "EXERCISE".equals(item.blockType())).findFirst()
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Exercise block was not found"));
        if (block.isEssay()) {
            throw new LearningRequestException(409, "ESSAY_BLOCK", "Essay blocks are submitted as essays");
        }
        List<Question> questions = orderedQuestions(block);
        Map<UUID, Object> answers = AnswerSheet.require(
                questions.stream().map(Question::questionVersionId).toList(), command.answers());
        List<AnswerSpecGrader.Grade> grades = questions.stream()
                .map(question -> grader.grade(question.answerSpec(), answers.get(question.questionVersionId()))).toList();
        if (grades.stream().anyMatch(grade -> !grade.gradable())) {
            throw new LearningRequestException(422, "UNGRADABLE_EXERCISE", "Exercise contains unsupported questions");
        }
        boolean firstSubmission = !store.hasSubmission(userId, lessonId, blockId);
        Set<UUID> knownKps = firstSubmission ? access.knownKnowledgePoints(userId, exerciseKnowledgePoints(lesson))
                : Set.of();
        // Refresh may change the first unpassed topic; authorize again before changing progress.
        if (firstSubmission) access.reauthorize(userId, context);
        access.refresh(userId, lesson);
        boolean blockPassed = AnswerSheet.passes(
                grades.stream().filter(AnswerSpecGrader.Grade::correct).count(), questions.size());
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
        // Essay blocks never gate completion: a learner without points can still finish every lesson.
        boolean lessonCompleted = completed(context.progress()) || lesson.blocks().stream()
                .filter(Block::isExercise).allMatch(item -> passed.contains(item.blockId()));
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
        Lesson lesson = access.authorize(userId, lessonId).lesson();
        if (lesson.blocks().stream().anyMatch(Block::isExercise)) {
            throw new LearningRequestException(409, "LESSON_HAS_EXERCISES", "Lesson contains exercise blocks");
        }
        access.refresh(userId, lesson);
        if (store.completeLesson(userId, lessonId)) reviews.execute(userId,
                new HashSet<>(lesson.knowledgePointIds()), Set.of());
        return lessonId;
    }

    private static Set<UUID> exerciseKnowledgePoints(Lesson lesson) {
        return lesson.blocks().stream().filter(Block::isExercise)
                .flatMap(block -> block.questions().stream()).flatMap(question -> question.knowledgePointIds().stream())
                .collect(Collectors.toSet());
    }
    private void reevaluate(UUID userId, Lesson lesson) {
        Map<UUID, List<UUID>> kpsByQuestion = lesson.blocks().stream()
                .filter(Block::isExercise).flatMap(block -> block.questions().stream())
                .collect(Collectors.toMap(Question::questionVersionId, Question::knowledgePointIds, (first, second) -> first));
        Set<UUID> wrong = store.findFirstSubmissions(userId, lesson.lessonId()).stream()
                .flatMap(submission -> submission.results().stream()).filter(result -> !result.correct())
                .flatMap(result -> kpsByQuestion.getOrDefault(result.questionVersionId(), List.of()).stream())
                .collect(Collectors.toSet());
        reviews.execute(userId, new HashSet<>(lesson.knowledgePointIds()), wrong);
    }

    private LearningRequestException requestConflict() {
        return new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");
    }

    private static boolean completed(LessonProgress progress) { return LessonAccess.completed(progress); }
    private Set<UUID> passedBlocks(LessonProgress progress) { return progress == null ? Set.of() : progress.passedBlockIds(); }
    private List<Block> orderedBlocks(Lesson lesson) {
        return lesson.blocks().stream().sorted(Comparator.comparingInt(Block::sortOrder)
                .thenComparing(block -> block.blockId().toString())).toList();
    }
    private List<Question> orderedQuestions(Block block) {
        return block.questions().stream().sorted(Comparator.comparingInt(Question::sortOrder)
                .thenComparing(question -> question.questionVersionId().toString())).toList();
    }

    private LessonResult.Block blockResult(Block block, boolean passed, boolean lessonCompleted,
                                           WritingSubmissionStore.BlockSummary essay) {
        var asset = block.asset() == null ? null : learnerAsset(block.asset(), lessonCompleted);
        if (block.isEssay()) {
            // The model answer stays hidden until the learner has passed the block once.
            boolean essayPassed = essay != null && essay.passed();
            var latest = essay == null ? null : new LessonResult.LatestSubmission(essay.latestId(),
                    essay.latestStatus(), essay.latestOverallBand(), essay.latestPassed());
            return new LessonResult.Block(block.blockId(), block.blockType(), "ESSAY", block.sortOrder(), null, null,
                    null, null, null, null, essayQuestion(block), latest,
                    essayPassed ? block.questions().getFirst().explanation() : null);
        }
        boolean exercise = block.isExercise();
        var questions = exercise ? orderedQuestions(block).stream().map(question -> new LessonResult.Question(
                question.questionVersionId(), question.sortOrder(), question.stem(), question.options() == null ? null
                : question.options().stream().map(option -> new LessonResult.Option(option.optionKey(), option.content(),
                        option.sortOrder())).toList())).toList() : null;
        var solutions = exercise && passed ? orderedQuestions(block).stream().map(question -> new LessonResult.Solution(
                question.questionVersionId(), grader.grade(question.answerSpec(), null).correctAnswer(),
                question.explanation())).toList() : null;
        return new LessonResult.Block(block.blockId(), block.blockType(), exercise ? "EXERCISE" : null,
                block.sortOrder(), block.textContent(), asset, block.vocabularySenseIds(), exercise ? passed : null,
                questions, solutions, null);
    }

    /** Passage text is shown as is; an audio transcript gives the answers away, so it waits for completion. */
    private static LessonResult.Asset learnerAsset(Asset asset, boolean lessonCompleted) {
        if ("PASSAGE".equals(asset.assetType())) {
            return new LessonResult.Asset(asset.id(), asset.assetType(), asset.textContent(), null, null, null);
        }
        String transcript = "AUDIO".equals(asset.assetType()) && lessonCompleted ? asset.textContent() : null;
        return new LessonResult.Asset(asset.id(), asset.assetType(), null, asset.mediaUrl(), asset.durationSeconds(),
                transcript);
    }

    /** The learner sees the prompt and images only; never the answer spec, chart facts or model answer. */
    private static LessonResult.EssayQuestion essayQuestion(Block block) {
        Question question = block.questions().getFirst();
        Map<String, Object> spec = question.answerSpec() == null ? Map.of() : question.answerSpec();
        List<LessonResult.Image> images = question.assets() == null ? List.of() : question.assets().stream()
                .filter(asset -> "IMAGE".equals(asset.assetType()))
                .sorted(Comparator.comparingInt(QuestionAsset::sortOrder))
                .map(asset -> new LessonResult.Image(asset.mediaUrl(), asset.altText())).toList();
        return new LessonResult.EssayQuestion(question.questionVersionId(), question.stem(),
                spec.get("task") instanceof String task ? task : null,
                spec.get("minWords") instanceof Number words ? words.intValue() : null,
                spec.get("passBand") instanceof Number band ? new BigDecimal(band.toString()) : null,
                images);
    }
}
