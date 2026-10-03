package com.group01.learning.application.usecase;

import com.group01.learning.application.port.ExerciseSubmissionLog;
import com.group01.learning.application.port.LearnerLock;
import com.group01.learning.application.port.LearningContentClient.Block;
import com.group01.learning.application.port.LearningContentClient.Lesson;
import com.group01.learning.application.result.LessonResult;
import com.group01.learning.application.service.LessonAccess;
import com.group01.learning.application.service.LessonViewAssembler;
import com.group01.learning.domain.repository.WritingSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/** Opens a lesson behind the lesson gate and shows it with what the learner has earned so far. */
@Service
public class GetLessonUseCase {
    private final LearnerLock lock;
    private final LessonAccess access;
    private final ExerciseSubmissionLog submissions;
    private final WritingSubmissionRepository essays;
    private final LessonViewAssembler view;

    public GetLessonUseCase(LearnerLock lock, LessonAccess access, ExerciseSubmissionLog submissions,
                            WritingSubmissionRepository essays, LessonViewAssembler view) {
        this.lock = lock;
        this.access = access;
        this.submissions = submissions;
        this.essays = essays;
        this.view = view;
    }

    @Transactional
    public LessonResult execute(UUID userId, UUID lessonId) {
        lock.lock(userId);
        LessonAccess.Context context = access.authorize(userId, lessonId);
        access.refresh(userId, context.lesson(), context.progress());
        boolean completed = LessonAccess.completed(context.progress());
        Set<UUID> passed = context.progress() == null ? Set.of() : context.progress().passedBlockIds();
        Lesson lesson = context.lesson();
        var blocks = view.blocks(lesson, passed, completed,
                essays.summarizeBlocks(userId, lesson.blocks().stream().filter(Block::isEssay).map(Block::blockId)
                        .toList()),
                submissions.wrongQuestions(userId, lessonId));
        return new LessonResult(lesson.lessonId(), lesson.topicId(), lesson.code(), lesson.title(), lesson.summary(),
                lesson.sortOrder(), completed ? "COMPLETED" : "AVAILABLE", blocks, lesson.skill());
    }
}
