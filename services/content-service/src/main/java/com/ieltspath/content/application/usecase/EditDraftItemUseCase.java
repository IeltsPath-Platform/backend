package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ieltspath.content.application.command.QuestionDraftItemPatch;
import com.ieltspath.content.application.command.QuestionDraftRequest;
import com.ieltspath.content.application.port.QuestionStemReader;
import com.ieltspath.content.application.exception.SectionDraftException;
import com.ieltspath.content.application.service.QuestionDraftItems;
import com.ieltspath.content.application.service.QuestionDraftValidator;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.aggregate.ErrorType;
import com.ieltspath.content.domain.exception.AiDraftNotFoundException;
import com.ieltspath.content.domain.exception.DraftItemNotFoundException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.exception.InvalidDraftItemException;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import com.ieltspath.content.domain.vo.DraftKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;

/**
 * Lets the author change or drop one question of a draft that is still waiting for review. An edited question is
 * checked by the same rules as a generated one, including a stem the knowledge point or the draft already has. Items
 * are addressed by position, so after a removal the items behind it move up by one.
 */
@Service
@Transactional
public class EditDraftItemUseCase {

    private final AiDraftRepository drafts;
    private final ErrorTypeRepository errorTypes;
    private final QuestionStemReader stems;
    private final ObjectMapper json;

    public EditDraftItemUseCase(AiDraftRepository drafts, ErrorTypeRepository errorTypes, QuestionStemReader stems,
                                ObjectMapper json) {
        this.drafts = drafts;
        this.errorTypes = errorTypes;
        this.stems = stems;
        this.json = json;
    }

    public AiDraft edit(UUID author, UUID draftId, int index, QuestionDraftItemPatch patch) {
        AiDraft draft = readyDraft(author, draftId);
        ObjectNode result = result(draft);
        ArrayNode items = items(result);
        ensureMetadata(draft, result, items);
        item(items, index);
        return editAt(draft, result, items, index, patch);
    }

    public AiDraft remove(UUID author, UUID draftId, int index) {
        AiDraft draft = readyDraft(author, draftId);
        ObjectNode result = result(draft);
        ArrayNode items = items(result);
        ensureMetadata(draft, result, items);
        item(items, index);
        items.remove(index);
        result.put("revision", result.path("revision").asInt() + 1);
        draft.replaceResult(write(result));
        return drafts.save(draft);
    }

    public AiDraft edit(UUID author, UUID draftId, UUID itemId, int revision, QuestionDraftItemPatch patch) {
        AiDraft draft = readyDraft(author, draftId);
        ObjectNode result = result(draft);
        ArrayNode items = items(result);
        ensureMetadata(draft, result, items);
        requireRevision(result, revision);
        int index = indexOf(items, itemId);
        if (index < 0) throw new DraftItemNotFoundException(-1);
        AiDraft saved = editAt(draft, result, items, index, patch);
        return saved;
    }

    public AiDraft remove(UUID author, UUID draftId, UUID itemId, int revision) {
        AiDraft draft = readyDraft(author, draftId);
        ObjectNode result = result(draft);
        ArrayNode items = items(result);
        ensureMetadata(draft, result, items);
        requireRevision(result, revision);
        int index = indexOf(items, itemId);
        if (index < 0) throw new DraftItemNotFoundException(-1);
        items.remove(index);
        result.put("revision", revision + 1);
        draft.replaceResult(write(result));
        return drafts.save(draft);
    }

    private AiDraft editAt(AiDraft draft, ObjectNode result, ArrayNode items, int index,
                           QuestionDraftItemPatch patch) {
        JsonNode current = items.get(index);
        QuestionDraftRequest request = request(draft);
        ObjectNode candidate = QuestionDraftItems.toCandidate(current);
        apply(candidate, patch);
        Set<String> known = stems.stemsOf(request.knowledgePointId()).stream().map(QuestionDraftItems::normalize)
                .collect(Collectors.toCollection(HashSet::new));
        for (int other = 0; other < items.size(); other++) if (other != index) known.add(QuestionDraftItems.normalize(items.get(other).path("stem").asText()));
        Set<String> codes = errorTypes.findApprovedActive().stream().map(ErrorType::getCode).collect(Collectors.toSet());
        String reason = QuestionDraftValidator.rejectionReason(candidate, request.questionType(), codes, known);
        if (reason != null) throw new InvalidDraftItemException(reason);
        ObjectNode replacement = QuestionDraftItems.toItem(candidate, request.questionType(), request.difficulty());
        replacement.put("itemId", current.path("itemId").asText());
        items.set(index, replacement);
        result.put("revision", result.path("revision").asInt() + 1);
        draft.replaceResult(write(result));
        return drafts.save(draft);
    }

    private void ensureMetadata(AiDraft draft, ObjectNode result, ArrayNode items) {
        boolean changed = !result.path("revision").canConvertToInt();
        if (changed) result.put("revision", 0);
        for (int index = 0; index < items.size(); index++) {
            JsonNode item = items.get(index);
            if (item.isObject() && !item.path("itemId").isTextual()) {
                ((ObjectNode) item).put("itemId", UUID.nameUUIDFromBytes((draft.getId() + ":" + index)
                        .getBytes(StandardCharsets.UTF_8)).toString());
                changed = true;
            }
        }
        if (changed) { draft.replaceResult(write(result)); drafts.save(draft); }
    }

    private int indexOf(ArrayNode items, UUID itemId) {
        for (int index = 0; index < items.size(); index++) if (itemId.toString().equals(items.get(index).path("itemId").asText())) return index;
        return -1;
    }

    private void requireRevision(ObjectNode result, int revision) {
        int current = result.path("revision").asInt(0);
        if (revision != current) throw new SectionDraftException(HttpStatus.CONFLICT, "DRAFT_CHANGED",
                "Bản nháp đã được thay đổi. Tải lại rồi thử lại.", java.util.Map.of());
    }

    private static void apply(ObjectNode candidate, QuestionDraftItemPatch patch) {
        if (patch.stem() != null) candidate.put("stem", patch.stem());
        if (patch.explanation() != null) candidate.put("explanation", patch.explanation());
        if (patch.options() != null) {
            ArrayNode options = candidate.putArray("options");
            patch.options().forEach(option -> options.addObject().put("optionKey", option.optionKey())
                    .put("content", option.content()));
        }
        if (patch.correct() != null) candidate.put("correct", patch.correct());
        if (patch.distractors() != null) {
            ObjectNode distractors = candidate.putObject("distractors");
            patch.distractors().forEach(distractors::put);
        }
        if (patch.accepted() != null) {
            ArrayNode accepted = candidate.putArray("accepted");
            patch.accepted().forEach(accepted::add);
        }
    }

    private AiDraft readyDraft(UUID author, UUID draftId) {
        AiDraft draft = drafts.findByIdForUpdate(draftId)
                .filter(found -> found.getRequestedBy().equals(author))
                .orElseThrow(() -> new AiDraftNotFoundException(draftId));
        if (draft.getKind() != DraftKind.QUESTIONS) {
            throw new InvalidContentStateException("Bản nháp này không phải bản nháp câu hỏi");
        }
        return draft;
    }

    private ObjectNode result(AiDraft draft) {
        try {
            JsonNode parsed = draft.getResultJson() == null ? null : json.readTree(draft.getResultJson());
            if (parsed instanceof ObjectNode object) return object;
        } catch (JsonProcessingException invalid) {
            // falls through to the same refusal as a result that is not an object
        }
        throw new InvalidContentStateException("Không đọc được kết quả bản nháp đã lưu");
    }

    private ArrayNode items(ObjectNode result) {
        if (result.path("items") instanceof ArrayNode items) return items;
        throw new InvalidContentStateException("Kết quả bản nháp đã lưu không có câu hỏi nào");
    }

    private JsonNode item(ArrayNode items, int index) {
        if (index < 0 || index >= items.size()) {
            throw new DraftItemNotFoundException(index);
        }
        return items.get(index);
    }

    private QuestionDraftRequest request(AiDraft draft) {
        try {
            return json.readValue(draft.getRequestJson(), QuestionDraftRequest.class);
        } catch (JsonProcessingException invalid) {
            throw new InvalidContentStateException("Không đọc được yêu cầu bản nháp đã lưu");
        }
    }

    private String write(JsonNode node) {
        try {
            return json.writeValueAsString(node);
        } catch (JsonProcessingException invalid) {
            throw new IllegalStateException("Không ghi được kết quả bản nháp", invalid);
        }
    }
}
