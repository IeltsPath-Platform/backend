package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ieltspath.content.application.command.AddQuestionVersionCommand;
import com.ieltspath.content.application.command.CreateQuestionCommand;
import com.ieltspath.content.application.command.QuestionDraftRequest;
import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.exception.AiDraftNotFoundException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.repository.LessonAuthoringRepository;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.application.exception.SectionDraftException;
import com.ieltspath.content.domain.aggregate.Lesson;
import com.ieltspath.content.domain.aggregate.LessonBlock;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.vo.BlockType;
import com.ieltspath.content.domain.vo.LessonStatus;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.QuestionOptionPayload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Turns the chosen items of a ready draft into ordinary DRAFT questions, through the same use cases every other
 * question goes through. All or nothing: one item that fails leaves the draft READY and creates nothing.
 */
@Service
@Transactional
public class ApproveQuestionDraftUseCase {

    private final AiDraftRepository drafts;
    private final CreateQuestionUseCase createQuestion;
    private final AddQuestionVersionUseCase addVersion;
    private final ObjectMapper json;
    private final Clock clock;
    private final LessonAuthoringRepository lessons;
    private final QuestionRepository questions;
    private final com.ieltspath.content.application.port.QuestionStemReader stems;
    private final KnowledgePointRepository knowledgePoints;

    @Autowired
    public ApproveQuestionDraftUseCase(AiDraftRepository drafts, CreateQuestionUseCase createQuestion,
                                       AddQuestionVersionUseCase addVersion, ObjectMapper json, Clock clock,
                                       LessonAuthoringRepository lessons, QuestionRepository questions,
                                       com.ieltspath.content.application.port.QuestionStemReader stems,
                                       KnowledgePointRepository knowledgePoints) {
        this.drafts = drafts;
        this.createQuestion = createQuestion;
        this.addVersion = addVersion;
        this.json = json;
        this.clock = clock;
        this.lessons = lessons;
        this.questions = questions;
        this.stems = stems;
        this.knowledgePoints = knowledgePoints;
    }

    public ApproveQuestionDraftUseCase(AiDraftRepository drafts, CreateQuestionUseCase createQuestion,
                                       AddQuestionVersionUseCase addVersion, ObjectMapper json, Clock clock) {
        this(drafts, createQuestion, addVersion, json, clock, null, null, null, null);
    }

    public AiDraft execute(UUID reviewer, UUID draftId, List<Integer> itemIndexes) {
        AiDraft draft = drafts.findByIdForUpdate(draftId)
                .filter(found -> found.getRequestedBy().equals(reviewer))
                .orElseThrow(() -> new AiDraftNotFoundException(draftId));
        if (draft.getKind() != DraftKind.QUESTIONS) {
            throw new InvalidContentStateException("Bản nháp này không phải bản nháp câu hỏi");
        }
        if (draft.getStatus() != DraftStatus.READY) {
            throw new InvalidContentStateException("Bản nháp đang ở trạng thái " + draft.getStatus() + ", cần ở trạng thái READY");
        }
        if (itemIndexes == null || itemIndexes.isEmpty()) {
            throw new IllegalArgumentException("Cần chọn ít nhất một câu");
        }
        JsonNode items = read(draft.getResultJson()).path("items");
        TreeSet<Integer> chosen = new TreeSet<>(itemIndexes);
        if (chosen.first() < 0 || chosen.last() >= items.size()) {
            throw new IllegalArgumentException("Có câu được chọn không tồn tại trong bản nháp");
        }
        QuestionDraftRequest request = readRequest(draft);

        draft.approve(reviewer, clock.instant());
        for (int index : chosen) {
            createFrom(items.get(index), request, draftId, index);
        }
        return drafts.save(draft);
    }

    @Transactional
    public AiDraft execute(UUID reviewer, UUID draftId, List<UUID> itemIds, int revision) {
        return execute(reviewer, draftId, itemIds, revision, false);
    }

    @Transactional
    public AiDraft execute(UUID reviewer, UUID draftId, List<UUID> itemIds, int revision, boolean createNewBlock) {
        AiDraft snapshot = drafts.findById(draftId).filter(d -> d.getRequestedBy().equals(reviewer))
                .orElseThrow(() -> new AiDraftNotFoundException(draftId));
        if (snapshot.getKind() != DraftKind.QUESTIONS) throw new InvalidContentStateException("Bản nháp này không phải bản nháp câu hỏi");
        QuestionDraftRequest storedRequest = readRequest(snapshot);
        Lesson lesson = null;
        if (storedRequest.lessonId() != null) {
            if (lessons == null) throw new InvalidContentStateException("Không thể duyệt câu hỏi cho bài học.");
            lesson = lessons.lockAndFindById(storedRequest.lessonId())
                    .orElseThrow(() -> new com.ieltspath.content.domain.exception.LessonNotFoundException(storedRequest.lessonId()));
        }
        AiDraft draft = drafts.findByIdForUpdate(draftId).filter(found -> found.getRequestedBy().equals(reviewer))
                .orElseThrow(() -> new AiDraftNotFoundException(draftId));
        if (lesson != null && lesson.getStatus() != LessonStatus.DRAFT) {
            throw new SectionDraftException(HttpStatus.CONFLICT, "LESSON_CHANGED",
                    "Bài học đã được xuất bản hoặc lưu trữ.", java.util.Map.of());
        }
        if (draft.getStatus() == DraftStatus.APPROVED) return alreadyApproved(draft, itemIds);
        if (draft.getStatus() != DraftStatus.READY) throw new InvalidContentStateException("Bản nháp chưa sẵn sàng để duyệt.");
        ObjectNode result = objectResult(draft);
        ArrayNode items = (ArrayNode) result.path("items");
        prepareMetadata(draft, result, items);
        int currentRevision = result.path("revision").asInt();
        if (revision != currentRevision) throw new InvalidContentStateException("Bản nháp đã được thay đổi. Tải lại rồi thử lại.");
        if (itemIds == null || itemIds.isEmpty() || new java.util.HashSet<>(itemIds).size() != itemIds.size())
            throw new IllegalArgumentException("Cần chọn ít nhất một câu hỏi khác nhau.");
        java.util.Map<UUID, JsonNode> byId = new java.util.LinkedHashMap<>();
        for (JsonNode item : items) try { byId.put(UUID.fromString(item.path("itemId").asText()), item); }
        catch (IllegalArgumentException ignored) { throw new InvalidContentStateException("Câu hỏi trong bản nháp không hợp lệ."); }
        if (!byId.keySet().containsAll(itemIds)) throw new IllegalArgumentException("Có câu được chọn không còn trong bản nháp.");

        QuestionDraftRequest request = readRequest(draft);
        if (lesson == null) {
            ObjectNode approved = json.createObjectNode();
            for (UUID itemId : itemIds) {
                createFrom(byId.get(itemId), request, draftId, indexOfItem(items, itemId));
                approved.put(itemId.toString(), "approved");
            }
            result.set("approved", approved);
            draft.replaceResult(write(result));
            draft.approve(reviewer, clock.instant());
            return drafts.save(draft);
        }
        lesson.ensureExerciseEditable();
        KnowledgePoint currentPoint = knowledgePoints == null ? null : knowledgePoints.findById(request.knowledgePointId()).orElse(null);
        if (!lesson.getKnowledgePointIds().contains(request.knowledgePointId()) || currentPoint == null || currentPoint.getStatus() != ContentStatus.ACTIVE)
            throw new SectionDraftException(HttpStatus.CONFLICT, "LESSON_CHANGED",
                    "Điểm kiến thức không còn khả dụng trong bài học.", java.util.Map.of("reason", "KNOWLEDGE_POINT_CHANGED"));
        LessonBlock target = null;
        if (request.blockId() != null && !createNewBlock) {
            target = lesson.getBlocks().stream().filter(block -> block.getId().equals(request.blockId())).findFirst()
                    .orElseThrow(() -> new SectionDraftException(HttpStatus.CONFLICT, "BLOCK_MISSING",
                            "Khối bài tập không còn tồn tại.", java.util.Map.of()));
            if (target.getType() != BlockType.EXERCISE) throw new InvalidContentStateException("Khối đích không phải khối bài tập.");
        }
        Skill skill = lessons.courseTopicSkillForLesson(lesson.getId()).orElse(null);
        if (skill == null || skill == Skill.ALL) skill = lessons.knowledgePointSkills(List.of(request.knowledgePointId())).get(request.knowledgePointId());
        if (skill == Skill.ALL) skill = null;
        List<String> existingStems = stems.stemsOf(request.knowledgePointId());
        for (UUID itemId : itemIds) {
            JsonNode item = byId.get(itemId);
            String stem = item.path("stem").asText();
            if (existingStems.stream().anyMatch(existing -> com.ieltspath.content.application.service.QuestionSimilarity.jaccard(stem, existing) >= 0.78))
                throw new InvalidContentStateException("Một số câu đã trùng với câu đang có tại điểm kiến thức.");
            existingStems = new ArrayList<>(existingStems); existingStems.add(stem);
        }
        List<UUID> versionIds = new ArrayList<>();
        for (UUID itemId : itemIds) {
            JsonNode item = byId.get(itemId);
            QuestionResult created = createQuestion.execute(new CreateQuestionCommand(request.questionType(), skill, null,
                    QuestionPurpose.LEARNING, "ai-" + draftId + "-" + itemId));
            addVersion.execute(new AddQuestionVersionCommand(created.id(), 1, item.path("stem").asText(), options(item),
                    item.path("answerSpec").toString(), item.path("explanation").asText(), null, request.difficulty(),
                    List.of(new AddQuestionVersionCommand.KnowledgePointInput(null, request.knowledgePointId(), BigDecimal.ONE))));
            Question saved = questions.findById(created.id()).orElseThrow();
            versionIds.add(saved.getVersions().getLast().getId());
        }
        List<UUID> allVersions = new ArrayList<>(target == null ? List.of() : target.getQuestionVersionIds());
        allVersions.addAll(versionIds);
        if (target == null) {
            int order = lesson.getBlocks().stream().mapToInt(LessonBlock::getSortOrder).max().orElse(-1) + 1;
            lesson.addBlock(LessonBlock.create(lesson.getId(), BlockType.EXERCISE, order, null, null, List.of(), allVersions, List.of()));
        } else {
            target.update(null, null, null, List.of(), allVersions, List.of());
        }
        if (!lessons.questionVersionsOwnedElsewhere(target == null ? null : target.getId(), versionIds).isEmpty())
            throw new InvalidContentStateException("Một số câu hỏi đã được dùng ở nơi khác.");
        lessons.save(lesson);
        ObjectNode approved = json.createObjectNode();
        for (int i = 0; i < itemIds.size(); i++) approved.put(itemIds.get(i).toString(), versionIds.get(i).toString());
        result.set("approved", approved);
        draft.replaceResult(write(result));
        draft.approve(reviewer, clock.instant());
        return drafts.save(draft);
    }

    private AiDraft alreadyApproved(AiDraft draft, List<UUID> itemIds) {
        JsonNode approved;
        try { approved = read(draft.getResultJson()).path("approved"); }
        catch (RuntimeException invalid) { approved = json.createObjectNode(); }
        java.util.Set<UUID> previously = new java.util.HashSet<>();
        if (approved.isObject()) approved.fieldNames().forEachRemaining(key -> {
            try { previously.add(UUID.fromString(key)); }
            catch (IllegalArgumentException ignored) { }
        });
        if (itemIds != null && !itemIds.isEmpty() && new java.util.HashSet<>(itemIds).size() == itemIds.size()
                && previously.equals(new java.util.HashSet<>(itemIds))) return draft;
        throw new SectionDraftException(HttpStatus.CONFLICT, "DRAFT_ALREADY_REVIEWED",
                "Bản nháp đã được duyệt với danh sách câu khác.", java.util.Map.of());
    }

    private ObjectNode objectResult(AiDraft draft) {
        JsonNode parsed = read(draft.getResultJson());
        if (parsed instanceof ObjectNode object) return object;
        throw new InvalidContentStateException("Không đọc được kết quả bản nháp đã lưu.");
    }

    private int indexOfItem(ArrayNode items, UUID itemId) {
        for (int index = 0; index < items.size(); index++)
            if (itemId.toString().equals(items.get(index).path("itemId").asText())) return index;
        throw new InvalidContentStateException("Câu hỏi trong bản nháp không hợp lệ.");
    }
    private void prepareMetadata(AiDraft draft, ObjectNode result, ArrayNode items) {
        boolean changed = !result.path("revision").canConvertToInt();
        if (changed) result.put("revision", 0);
        for (int index = 0; index < items.size(); index++) if (!items.get(index).path("itemId").isTextual()) {
            ((ObjectNode) items.get(index)).put("itemId", UUID.nameUUIDFromBytes((draft.getId()+":"+index).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString()); changed = true;
        }
        if (changed) draft.replaceResult(write(result));
    }

    private void createFrom(JsonNode item, QuestionDraftRequest request, UUID draftId, int index) {
        Skill skill = lessons == null ? null : lessons.knowledgePointSkills(List.of(request.knowledgePointId())).get(request.knowledgePointId());
        if (skill == Skill.ALL) skill = null;
        QuestionResult question = createQuestion.execute(new CreateQuestionCommand(
                request.questionType(), skill, null, QuestionPurpose.LEARNING, "ai-" + draftId + "-" + index));
        addVersion.execute(new AddQuestionVersionCommand(
                question.id(), 1, item.path("stem").asText(), options(item), item.path("answerSpec").toString(),
                item.path("explanation").asText(), null, request.difficulty(),
                List.of(new AddQuestionVersionCommand.KnowledgePointInput(null, request.knowledgePointId(),
                        BigDecimal.ONE))));
    }

    private List<QuestionOptionPayload> options(JsonNode item) {
        List<QuestionOptionPayload> options = new ArrayList<>();
        for (JsonNode option : item.path("options")) {
            options.add(new QuestionOptionPayload(option.path("optionKey").asText(),
                    option.path("content").asText(), option.path("sortOrder").asInt()));
        }
        return options;
    }

    private JsonNode read(String text) {
        try {
            return text == null ? json.createObjectNode() : json.readTree(text);
        } catch (JsonProcessingException invalid) {
            throw new InvalidContentStateException("Không đọc được kết quả bản nháp đã lưu");
        }
    }

    private QuestionDraftRequest readRequest(AiDraft draft) {
        try {
            return json.readValue(draft.getRequestJson(), QuestionDraftRequest.class);
        } catch (JsonProcessingException invalid) {
            throw new InvalidContentStateException("Không đọc được yêu cầu bản nháp đã lưu");
        }
    }

    private String write(JsonNode node) {
        try { return json.writeValueAsString(node); }
        catch (JsonProcessingException invalid) { throw new InvalidContentStateException("Không ghi được kết quả bản nháp."); }
    }
}
