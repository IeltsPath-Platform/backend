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
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.entity.QuestionVersion;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.BlockType;
import com.ieltspath.content.domain.vo.LessonAuthoringSnapshot;
import com.ieltspath.content.domain.vo.LessonStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
public class LessonAuthoringUseCase {
    private final LessonAuthoringRepository repository;
    private final VocabularySenseLookup vocabulary;
    private final QuestionRepository questions;
    private final PublishQuestionVersionUseCase publisher;

    @Autowired
    public LessonAuthoringUseCase(LessonAuthoringRepository repository, VocabularySenseLookup vocabulary,
                                  QuestionRepository questions, PublishQuestionVersionUseCase publisher) {
        this.repository = repository;
        this.vocabulary = vocabulary;
        this.questions = questions;
        this.publisher = publisher;
    }

    public LessonAuthoringUseCase(LessonAuthoringRepository repository, VocabularySenseLookup vocabulary) {
        this(repository, vocabulary, null, null);
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
        requireCourseSkill(lessonId, knowledgePointIds);
        lesson.replaceKnowledgePoints(knowledgePointIds);
        repository.save(lesson);
    }

    @Transactional
    public LessonAuthoringResult addBlock(UUID lessonId, BlockType type, Integer sortOrder, String textContent,
                                          UUID assetId, List<UUID> vocabularySenseIds,
                                          List<UUID> questionVersionIds, List<UUID> knowledgePointIds) {
        Lesson lesson = requireLocked(lessonId);
        if (type == BlockType.EXERCISE) lesson.ensureExerciseEditable();
        requireCourseSkill(lessonId, knowledgePointIds);
        validateBlock(lessonId, type, sortOrder, textContent, assetId, vocabularySenseIds,
                questionVersionIds, knowledgePointIds);
        if (type == BlockType.EXERCISE) requireQuestionsFree(null, questionVersionIds);
        // After the last block, whatever its number: lessons seeded from 1 (or with gaps after deletions) have a block at
        // the number of blocks, and giving a new block that same position broke the unique (lesson, sort order) key.
        int position = sortOrder != null ? sortOrder
                : lesson.getBlocks().stream().mapToInt(LessonBlock::getSortOrder).max().orElse(-1) + 1;
        lesson.addBlock(LessonBlock.create(lessonId, type, position, textContent, assetId,
                vocabularySenseIds, questionVersionIds, knowledgePointIds));
        repository.save(lesson);
        return get(lessonId);
    }

    @Transactional
    public LessonAuthoringResult updateBlock(UUID lessonId, UUID blockId, BlockType blockType, Integer sortOrder,
                                             String textContent, UUID assetId, List<UUID> vocabularySenseIds,
                                             List<UUID> questionVersionIds, List<UUID> knowledgePointIds) {
        return updateBlock(lessonId, blockId, blockType, sortOrder, textContent, assetId, vocabularySenseIds,
                questionVersionIds, knowledgePointIds, null);
    }

    @Transactional
    public LessonAuthoringResult updateBlock(UUID lessonId, UUID blockId, BlockType blockType, Integer sortOrder,
                                             String textContent, UUID assetId, List<UUID> vocabularySenseIds,
                                             List<UUID> questionVersionIds, List<UUID> knowledgePointIds,
                                             List<UUID> expectedQuestionVersionIds) {
        Lesson lesson = requireLocked(lessonId);
        LessonBlock block = lesson.requireBlock(blockId);
        if (block.getType() != blockType) {
            throw new LessonBlockInvalidException("Không đổi được loại khối");
        }
        requireCourseSkill(lessonId, knowledgePointIds);
        List<UUID> requestedQuestions = questionVersionIds == null ? List.of() : questionVersionIds;
        if (blockType == BlockType.EXERCISE && expectedQuestionVersionIds != null
                && !block.getQuestionVersionIds().equals(expectedQuestionVersionIds)) {
            throw new com.ieltspath.content.application.exception.SectionDraftException(
                    org.springframework.http.HttpStatus.CONFLICT, "BLOCK_CHANGED",
                    "Danh sách câu hỏi của khối đã thay đổi. Tải lại bài học rồi thử lại.", java.util.Map.of());
        }
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
    public int publishDraftQuestions(UUID lessonId) {
        Lesson lesson = requireLocked(lessonId);
        lesson.ensureExerciseEditable();
        List<UUID> ids = lesson.getBlocks().stream().filter(block -> block.getType() == BlockType.EXERCISE)
                .flatMap(block -> block.getQuestionVersionIds().stream()).distinct().toList();
        if (ids.isEmpty()) return 0;
        List<Question> locked = questions.findByVersionIdsForUpdate(ids);
        java.util.Map<UUID, Question> byVersion = new java.util.HashMap<>();
        locked.forEach(question -> question.getVersions().forEach(version -> byVersion.put(version.getId(), question)));
        if (!byVersion.keySet().containsAll(ids)) throw new LessonBlockInvalidException("Một số phiên bản câu hỏi không còn tồn tại.");
        List<QuestionVersion> drafts = ids.stream().map(byVersion::get).flatMap(question -> question.getVersions().stream()
                .filter(version -> ids.contains(version.getId()) && version.getStatus() == PublicationStatus.DRAFT)).toList();
        for (QuestionVersion version : drafts) {
            Question question = byVersion.get(version.getId());
            publisher.validateForPublishing(question.getQuestionType(), version);
        }
        for (QuestionVersion version : drafts) {
            Question question = byVersion.get(version.getId());
            question.publishVersion(version.getId());
            questions.save(question);
        }
        return drafts.size();
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

    private void requireCourseSkill(UUID lessonId, List<UUID> knowledgePointIds) {
        if (knowledgePointIds == null || knowledgePointIds.isEmpty()) return;
        var topicSkill = repository.courseTopicSkillForLesson(lessonId);
        if (topicSkill.isEmpty()) return;
        var skills = repository.knowledgePointSkills(knowledgePointIds);
        if (!skills.keySet().containsAll(knowledgePointIds)
                || skills.values().stream().anyMatch(skill -> skill != topicSkill.get()
                        && skill != com.ieltspath.content.domain.vo.Skill.ALL)) {
            throw new LessonBlockInvalidException("Điểm kiến thức của bài học phải cùng kỹ năng với chủ đề của khóa học");
        }
    }

    private void validateBlock(UUID lessonId, BlockType type, Integer sortOrder, String textContent, UUID assetId,
                               List<UUID> vocabularySenseIds, List<UUID> questionVersionIds,
                               List<UUID> knowledgePointIds) {
        LessonBlock block = LessonBlock.create(lessonId, type, sortOrder == null ? 0 : sortOrder, textContent,
                assetId, vocabularySenseIds, questionVersionIds, knowledgePointIds);
        if (type == BlockType.VOCABULARY
                && !vocabulary.findActiveSenseIds(block.getVocabularySenseIds()).containsAll(block.getVocabularySenseIds())) {
            throw new LessonBlockInvalidException("Khối từ vựng có nghĩa bị thiếu hoặc đã ngừng dùng");
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
