package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.BlockType;
import com.ieltspath.content.domain.vo.LessonStatus;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.LessonAuthoringSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LessonAuthoringResult(UUID id, UUID topicId, String code, String title, String summary,
                                    String summaryMarkdown, Integer estimatedMinutes, int sortOrder,
                                    LessonStatus status, Instant createdAt, Instant updatedAt,
                                    List<UUID> knowledgePointIds, List<Block> blocks) {
    public LessonAuthoringResult {
        knowledgePointIds = knowledgePointIds == null ? List.of() : List.copyOf(knowledgePointIds);
        blocks = blocks == null ? List.of() : List.copyOf(blocks);
    }

    public record Block(UUID id, int sortOrder, BlockType blockType, String textContent, UUID assetId,
                        List<UUID> vocabularySenseIds, List<UUID> knowledgePointIds, List<QuestionVersion> questions) {
        public Block {
            vocabularySenseIds = vocabularySenseIds == null ? List.of() : List.copyOf(vocabularySenseIds);
            knowledgePointIds = knowledgePointIds == null ? List.of() : List.copyOf(knowledgePointIds);
            questions = questions == null ? List.of() : List.copyOf(questions);
        }
    }

    public record QuestionVersion(UUID id, int versionNumber, QuestionType questionType, Skill skill,
                                  QuestionPurpose purpose, PublicationStatus status, String stem,
                                  String answerSpecJson) {}

    public static LessonAuthoringResult from(LessonAuthoringSnapshot snapshot) {
        List<Block> blocks = snapshot.blocks().stream().map(block -> new Block(block.id(), block.sortOrder(),
                block.blockType(), block.textContent(), block.assetId(), block.vocabularySenseIds(),
                block.knowledgePointIds(), block.questions().stream().map(question -> new QuestionVersion(
                        question.id(), question.versionNumber(), question.questionType(), question.skill(),
                        question.purpose(), question.status(), question.stem(), question.answerSpecJson())).toList())).toList();
        return new LessonAuthoringResult(snapshot.id(), snapshot.topicId(), snapshot.code(), snapshot.title(),
                snapshot.summary(), snapshot.summaryMarkdown(), snapshot.estimatedMinutes(), snapshot.sortOrder(),
                snapshot.status(), snapshot.createdAt(), snapshot.updatedAt(), snapshot.knowledgePointIds(), blocks);
    }
}
