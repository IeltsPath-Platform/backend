package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.command.QuestionDraftRequest;
import com.ieltspath.content.application.port.QuestionDraftLimits;
import com.ieltspath.content.application.service.QuestionDraftValidator;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.exception.KnowledgePointNotFoundException;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.repository.LessonAuthoringRepository;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.application.exception.SectionDraftException;
import org.springframework.http.HttpStatus;
import com.ieltspath.content.domain.vo.BlockType;
import com.ieltspath.content.domain.vo.LessonStatus;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.DraftKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

/** Validates an author's request for question drafts, then queues it against the author's allowance. */
@Service
@Transactional
public class RequestQuestionDraftUseCase {

    static final int MAX_INSTRUCTIONS = 500;

    private final KnowledgePointRepository knowledgePoints;
    private final StartAiDraftUseCase start;
    private final ObjectMapper json;
    private final QuestionDraftLimits limits;
    private final LessonAuthoringRepository lessons;
    private final AiDraftRepository drafts;

    @Autowired
    public RequestQuestionDraftUseCase(KnowledgePointRepository knowledgePoints, StartAiDraftUseCase start,
                                       ObjectMapper json, QuestionDraftLimits limits,
                                       LessonAuthoringRepository lessons, AiDraftRepository drafts) {
        this.knowledgePoints = knowledgePoints;
        this.start = start;
        this.json = json;
        this.limits = limits;
        this.lessons = lessons;
        this.drafts = drafts;
    }

    public RequestQuestionDraftUseCase(KnowledgePointRepository knowledgePoints, StartAiDraftUseCase start,
                                       ObjectMapper json, QuestionDraftLimits limits) {
        this(knowledgePoints, start, json, limits, null, null);
    }

    public AiDraft findOpen(UUID author, UUID lessonId) {
        return drafts.findOpenLessonQuestionDraft(author, lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Bài học chưa có bản nháp đang mở."));
    }

    public AiDraft execute(UUID author, QuestionDraftRequest request) {
        KnowledgePoint point = knowledgePoints.findById(request.knowledgePointId())
                .orElseThrow(() -> new KnowledgePointNotFoundException(request.knowledgePointId()));
        if (point.getStatus() != ContentStatus.ACTIVE) {
            throw new InvalidContentStateException("Điểm kiến thức đang ngừng dùng");
        }
        if (request.questionType() == null || !QuestionDraftValidator.supports(request.questionType())) {
            throw new IllegalArgumentException("AI chưa viết được loại câu hỏi này");
        }
        if (request.lessonId() != null) {
            if (lessons == null) throw new IllegalArgumentException("Không thể sinh câu hỏi cho bài học.");
            var lesson = lessons.lockAndFindById(request.lessonId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài học."));
            if (lesson.getStatus() != LessonStatus.DRAFT) throw new IllegalArgumentException("Bài học không còn là bản nháp.");
            if (!lesson.getKnowledgePointIds().contains(point.getId())) throw new IllegalArgumentException("Điểm kiến thức không thuộc bài học.");
            if (request.blockId() != null) {
                var block = lesson.requireBlock(request.blockId());
                if (block.getType() != BlockType.EXERCISE) throw new IllegalArgumentException("Khối đích không phải khối bài tập.");
            }
            var open = drafts.findOpenLessonQuestionDraft(author, request.lessonId());
            if (open.isPresent()) throw new SectionDraftException(HttpStatus.CONFLICT, "LESSON_DRAFT_OPEN",
                    "Bài học đã có một lượt sinh câu hỏi đang mở.", java.util.Map.of("draftId", open.get().getId()));
        } else if (request.blockId() != null) {
            throw new IllegalArgumentException("Cần chọn bài học cho khối bài tập.");
        }
        int maxItems = limits.maxItemsPerRequest();
        if (request.count() < 1 || request.count() > maxItems) {
            throw new IllegalArgumentException("Số câu phải từ 1 đến " + maxItems);
        }
        if (request.instructions() != null && request.instructions().length() > MAX_INSTRUCTIONS) {
            throw new IllegalArgumentException("Hướng dẫn thêm không được dài quá " + MAX_INSTRUCTIONS + " ký tự");
        }
        QuestionDraftRequest normalized = new QuestionDraftRequest(request.knowledgePointId(), request.questionType(),
                request.count(), com.ieltspath.content.domain.vo.QuestionDifficulty.MEDIUM, request.instructions(),
                request.lessonId(), request.blockId());
        return start.execute(author, DraftKind.QUESTIONS, body(normalized), limits.dailyLimit());
    }

    private String body(QuestionDraftRequest request) {
        try {
            return json.writeValueAsString(request);
        } catch (JsonProcessingException invalid) {
            throw new IllegalStateException("Không ghi được yêu cầu", invalid);
        }
    }
}
