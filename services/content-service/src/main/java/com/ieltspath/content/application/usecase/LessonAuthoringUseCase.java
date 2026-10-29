package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.VocabularySenseLookup;
import com.ieltspath.content.application.result.LessonAuthoringResult;
import com.ieltspath.content.application.result.LessonAuthoringSummaryResult;
import com.ieltspath.content.domain.aggregate.Lesson;
import com.ieltspath.content.domain.aggregate.LessonBlock;
import com.ieltspath.content.domain.exception.LessonBlockInvalidException;
import com.ieltspath.content.domain.exception.QuestionAlreadyOwnedException;
import com.ieltspath.content.domain.exception.LessonNotFoundException;
import com.ieltspath.content.domain.exception.LessonNotPublishableException;
import com.ieltspath.content.domain.exception.LessonSortConflictException;
import com.ieltspath.content.domain.exception.TopicNotFoundException;
import com.ieltspath.content.domain.repository.LessonAuthoringRepository;
import com.ieltspath.content.domain.vo.BlockType;
import com.ieltspath.content.domain.vo.LessonAuthoringSnapshot;
import com.ieltspath.content.domain.vo.LessonStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
public class LessonAuthoringUseCase {
    private final LessonAuthoringRepository repository;
    private final VocabularySenseLookup vocabulary;

    public LessonAuthoringUseCase(LessonAuthoringRepository repository, VocabularySenseLookup vocabulary) {
        this.repository = repository;
        this.vocabulary = vocabulary;
    }

    @Transactional(readOnly = true)
    public List<LessonAuthoringSummaryResult> list(UUID topicId) {
        requireTopic(topicId);
        return repository.findByTopic(topicId).stream().map(LessonAuthoringSummaryResult::from).toList();
    }

    @Transactional(readOnly = true)
    public LessonAuthoringResult get(UUID lessonId) {
        return repository.findSnapshotById(lessonId).map(LessonAuthoringResult::from)
                .orElseThrow(() -> new LessonNotFoundException(lessonId));
    }

    @Transactional
    public LessonAuthoringResult create(UUID topicId, String code, String title, String summary,
                                        String summaryMarkdown, Integer estimatedMinutes, Integer sortOrder) {
        requireTopic(topicId);
        Lesson lesson = repository.create(topicId, code, title, summary, summaryMarkdown, estimatedMinutes, sortOrder);
        return get(lesson.getId());
    }

    @Transactional
    public LessonAuthoringResult update(UUID lessonId, String title, String summary,
                                        String summaryMarkdown, Integer estimatedMinutes) {
        Lesson lesson = requireLocked(lessonId);
        lesson.update(title, summary, summaryMarkdown, estimatedMinutes);
        repository.save(lesson);
        return get(lessonId);
    }

    @Transactional
    public void reorderLessons(UUID topicId, List<UUID> lessonIds) {
        requireTopic(topicId);
        requireUniqueOrder(lessonIds);
        repository.reorderLessons(topicId, lessonIds);
    }

    @Transactional
    public void knowledgePoints(UUID lessonId, List<UUID> knowledgePointIds) {
        Lesson lesson = requireLocked(lessonId);
        lesson.replaceKnowledgePoints(knowledgePointIds);
        repository.save(lesson);
    }

    @Transactional
    public LessonAuthoringResult addBlock(UUID lessonId, BlockType type, Integer sortOrder, String textContent,
                                          UUID assetId, List<UUID> vocabularySenseIds,
                                          List<UUID> questionVersionIds, List<UUID> knowledgePointIds) {
        Lesson lesson = requireLocked(lessonId);
        if (type == BlockType.EXERCISE) lesson.ensureExerciseEditable();
        validateBlock(lessonId, type, sortOrder, textContent, assetId, vocabularySenseIds,
                questionVersionIds, knowledgePointIds);
        if (type == BlockType.EXERCISE) requireQuestionsFree(null, questionVersionIds);
        int position = sortOrder == null ? lesson.getBlocks().size() : sortOrder;
        lesson.addBlock(LessonBlock.create(lessonId, type, position, textContent, assetId,
                vocabularySenseIds, questionVersionIds, knowledgePointIds));
        repository.save(lesson);
        return get(lessonId);
    }

    @Transactional
    public LessonAuthoringResult updateBlock(UUID lessonId, UUID blockId, BlockType blockType, Integer sortOrder,
                                             String textContent, UUID assetId, List<UUID> vocabularySenseIds,
                                             List<UUID> questionVersionIds, List<UUID> knowledgePointIds) {
        Lesson lesson = requireLocked(lessonId);
        LessonBlock block = lesson.requireBlock(blockId);
        if (block.getType() != blockType) {
            throw new LessonBlockInvalidException("Block type cannot be changed");
        }
        List<UUID> requestedQuestions = questionVersionIds == null ? List.of() : questionVersionIds;
        if (blockType == BlockType.EXERCISE && !block.getQuestionVersionIds().equals(requestedQuestions)) {
            lesson.ensureExerciseEditable();
        }
        validateBlock(lessonId, blockType, sortOrder, textContent, assetId, vocabularySenseIds,
                questionVersionIds, knowledgePointIds);
        if (blockType == BlockType.EXERCISE) requireQuestionsFree(blockId, questionVersionIds);
        block.update(sortOrder, textContent, assetId,
                vocabularySenseIds, questionVersionIds, knowledgePointIds);
        repository.save(lesson);
        return get(lessonId);
    }

    @Transactional
    public void deleteBlock(UUID lessonId, UUID blockId) {
        Lesson lesson = requireLocked(lessonId);
        LessonBlock block = lesson.requireBlock(blockId);
        if (block.getType() == BlockType.EXERCISE) lesson.ensureExerciseEditable();
        lesson.removeBlock(blockId);
        repository.save(lesson);
    }

    @Transactional
    public void reorderBlocks(UUID lessonId, List<UUID> blockIds) {
        Lesson lesson = requireLocked(lessonId);
        lesson.reorderBlocks(blockIds);
        repository.save(lesson);
    }

    @Transactional
    public LessonAuthoringResult publish(UUID lessonId) {
        Lesson lesson = requireLocked(lessonId);
        List<String> issues = new java.util.ArrayList<>(repository.publicationViolations(lessonId));
        if (lesson.getBlocks().isEmpty()) issues.add("LESSON_HAS_NO_BLOCKS");
        for (LessonBlock block : lesson.getBlocks()) {
            if (block.getType() == BlockType.VOCABULARY
                    && !vocabulary.findActiveSenseIds(block.getVocabularySenseIds()).containsAll(block.getVocabularySenseIds())) {
                issues.add("VOCABULARY_SENSE_UNAVAILABLE");
            }
        }
        if (!issues.isEmpty()) throw new LessonNotPublishableException(issues.stream().distinct().toList());
        lesson.publish();
        repository.save(lesson);
        return get(lessonId);
    }

    @Transactional
    public LessonAuthoringResult archive(UUID lessonId) {
        Lesson lesson = requireLocked(lessonId);
        lesson.archive();
        repository.save(lesson);
        return get(lessonId);
    }

    private Lesson requireLocked(UUID lessonId) {
        return repository.lockAndFindById(lessonId).orElseThrow(() -> new LessonNotFoundException(lessonId));
    }

    private void requireTopic(UUID topicId) {
        if (!repository.topicExists(topicId)) throw new TopicNotFoundException(topicId);
    }

    private void validateBlock(UUID lessonId, BlockType type, Integer sortOrder, String textContent, UUID assetId,
                               List<UUID> vocabularySenseIds, List<UUID> questionVersionIds,
                               List<UUID> knowledgePointIds) {
        LessonBlock block = LessonBlock.create(lessonId, type, sortOrder == null ? 0 : sortOrder, textContent,
                assetId, vocabularySenseIds, questionVersionIds, knowledgePointIds);
        if (type == BlockType.VOCABULARY
                && !vocabulary.findActiveSenseIds(block.getVocabularySenseIds()).containsAll(block.getVocabularySenseIds())) {
            throw new LessonBlockInvalidException("Vocabulary block contains a missing or inactive sense");
        }
    }

    /** Every question of an exercise block must be free: one that has a home elsewhere is refused, and none is taken. */
    private void requireQuestionsFree(UUID blockId, List<UUID> questionVersionIds) {
        if (questionVersionIds == null || questionVersionIds.isEmpty()) return;
        List<UUID> taken = repository.questionVersionsOwnedElsewhere(blockId, questionVersionIds);
        if (!taken.isEmpty()) throw new QuestionAlreadyOwnedException(taken);
    }

    private void requireUniqueOrder(List<UUID> ids) {
        if (ids.size() != new HashSet<>(ids).size()) throw new LessonSortConflictException();
    }
}
