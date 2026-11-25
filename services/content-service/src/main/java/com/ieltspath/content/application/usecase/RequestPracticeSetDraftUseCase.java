package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.command.PracticeSetDraftRequest;
import com.ieltspath.content.application.service.QuestionDraftValidator;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.exception.KnowledgePointNotFoundException;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.infrastructure.config.ContentAiProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Validates an author's request for a practice set on the knowledge points of one lesson, then queues it against the
 * author's allowance. Only reading points are accepted: the model can write a passage, but not audio.
 */
@Service
@Transactional
public class RequestPracticeSetDraftUseCase {

    static final int MAX_POINTS = 5;
    static final int MIN_QUESTIONS = 3;
    static final int MAX_QUESTIONS = 10;
    static final int MAX_INSTRUCTIONS = 500;
    static final int MAX_LESSON_TITLE = 255;
    static final int MAX_VOCABULARY = 30;
    static final int MAX_WORD = 100;

    private final KnowledgePointRepository knowledgePoints;
    private final StartAiDraftUseCase start;
    private final ObjectMapper json;
    private final ContentAiProperties properties;

    public RequestPracticeSetDraftUseCase(KnowledgePointRepository knowledgePoints, StartAiDraftUseCase start,
                                          ObjectMapper json, ContentAiProperties properties) {
        this.knowledgePoints = knowledgePoints;
        this.start = start;
        this.json = json;
        this.properties = properties;
    }

    public AiDraft execute(UUID author, PracticeSetDraftRequest request) {
        List<UUID> ids = request.knowledgePointIds() == null ? List.of() : request.knowledgePointIds().stream()
                .distinct().toList();
        if (ids.isEmpty() || ids.size() > MAX_POINTS) {
            throw new IllegalArgumentException("Cần chọn từ 1 đến " + MAX_POINTS + " điểm kiến thức");
        }
        for (UUID id : ids) {
            KnowledgePoint point = knowledgePoints.findById(id).orElseThrow(() -> new KnowledgePointNotFoundException(id));
            if (point.getStatus() != ContentStatus.ACTIVE) {
                throw new InvalidContentStateException("Điểm kiến thức đang ngừng dùng");
            }
            if (point.getSkill() != Skill.READING) {
                throw new IllegalArgumentException("AI chỉ viết được bộ luyện tập cho điểm kiến thức Reading");
            }
        }
        if (request.questionType() == null || !QuestionDraftValidator.isChoice(request.questionType())) {
            throw new IllegalArgumentException("Loại câu hỏi phải là MULTIPLE_CHOICE hoặc TRUE_FALSE_NOT_GIVEN");
        }
        if (request.difficulty() == null) {
            throw new IllegalArgumentException("Cần chọn độ khó");
        }
        int maxQuestions = Math.min(MAX_QUESTIONS, properties.getAi().getMaxItemsPerRequest());
        if (request.count() < MIN_QUESTIONS || request.count() > maxQuestions) {
            throw new IllegalArgumentException("Số câu phải từ " + MIN_QUESTIONS + " đến " + maxQuestions);
        }
        if (request.instructions() != null && request.instructions().length() > MAX_INSTRUCTIONS) {
            throw new IllegalArgumentException("Hướng dẫn thêm không được dài quá " + MAX_INSTRUCTIONS + " ký tự");
        }
        if (request.lessonTitle() != null && request.lessonTitle().length() > MAX_LESSON_TITLE) {
            throw new IllegalArgumentException("Tên bài học không được dài quá " + MAX_LESSON_TITLE + " ký tự");
        }
        List<String> words = request.vocabulary().stream().map(String::strip).filter(word -> !word.isEmpty())
                .distinct().toList();
        if (words.size() > MAX_VOCABULARY || words.stream().anyMatch(word -> word.length() > MAX_WORD)) {
            throw new IllegalArgumentException("Danh sách từ vựng tối đa " + MAX_VOCABULARY + " từ, mỗi từ dài tối đa "
                    + MAX_WORD + " ký tự");
        }
        PracticeSetDraftRequest queued = new PracticeSetDraftRequest(ids, request.lessonTitle(), words,
                request.questionType(), request.count(), request.difficulty(), request.instructions());
        return start.execute(author, DraftKind.PRACTICE_SET, body(queued), properties.getAi().getDailyLimit());
    }

    private String body(PracticeSetDraftRequest request) {
        try {
            return json.writeValueAsString(request);
        } catch (JsonProcessingException invalid) {
            throw new IllegalStateException("Không ghi được yêu cầu", invalid);
        }
    }
}
