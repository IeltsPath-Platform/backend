package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ieltspath.content.application.command.AddQuestionVersionCommand;
import com.ieltspath.content.application.command.CreateQuestionCommand;
import com.ieltspath.content.application.exception.SectionDraftException;
import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.PackageVersionContentResult;
import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.application.result.SectionQuestionDraftDetail;
import com.ieltspath.content.application.result.SectionQuestionRules;
import com.ieltspath.content.application.service.QuestionDraftItems;
import com.ieltspath.content.application.service.SectionQuestionChecks;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.entity.ContentSection;
import com.ieltspath.content.domain.exception.AiDraftNotFoundException;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionOptionPayload;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SectionQuestionDraftReviewUseCase {
    private final AiDraftRepository drafts;
    private final ContentPackageRepository packages;
    private final LearningContentReader content;
    private final CreateQuestionUseCase createQuestion;
    private final AddQuestionVersionUseCase addVersion;
    private final AddSectionQuestionUseCase addToSection;
    private final QuestionRepository questions;
    private final ObjectMapper json;
    private final SectionQuestionRules rules;
    private final java.time.Clock clock;
    private final EntityManager entityManager;

    public SectionQuestionDraftReviewUseCase(AiDraftRepository drafts, ContentPackageRepository packages,
            LearningContentReader content, CreateQuestionUseCase createQuestion, AddQuestionVersionUseCase addVersion,
            AddSectionQuestionUseCase addToSection, QuestionRepository questions, ObjectMapper json,
            SectionQuestionRules rules, java.time.Clock clock, EntityManager entityManager) {
        this.drafts = drafts; this.packages = packages; this.content = content; this.createQuestion = createQuestion;
        this.addVersion = addVersion; this.addToSection = addToSection; this.questions = questions;
        this.json = json; this.rules = rules; this.clock = clock; this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public AiDraft get(UUID author, UUID id) { return owned(author, id); }

    @Transactional(readOnly = true)
    public SectionQuestionDraftDetail detail(UUID author, UUID id) {
        AiDraft draft = owned(author, id);
        RequestSectionQuestionDraftUseCase.StoredRequest request = request(draft);
        String state = "OK";
        boolean changed = false;
        try {
            ContentPackage pkg = packages.findById(request.packageId()).orElse(null);
            if (pkg == null) state = "MISSING";
            else {
                var version = pkg.requireVersion(request.versionId());
                if (version.getStatus() != PublicationStatus.DRAFT) state = "VERSION_NOT_DRAFT";
                else {
                    ContentSection section = pkg.requireSectionInVersion(request.versionId(), request.sectionId());
                    if (section.getSkill() != request.skill()) state = "SKILL_CHANGED";
                    PackageVersionContentResult latest = content.packageVersionContent(request.versionId()).orElse(null);
                    PackageVersionContentResult.Section current = latest == null ? null : latest.sections().stream()
                            .filter(s -> s.sectionId().equals(request.sectionId())).findFirst().orElse(null);
                    String source = current == null ? null : request.skill() == com.ieltspath.content.domain.vo.Skill.LISTENING
                            ? current.audio() == null ? null : current.audio().transcript() : current.passage();
                    if (source == null || !sha256(source.replace("\r\n", "\n").replace('\r', '\n').strip()).equals(request.source().sha256())) changed = true;
                }
            }
        } catch (com.ieltspath.content.domain.exception.ContentSectionNotFoundException missing) { state = "MISSING"; }
        catch (com.ieltspath.content.domain.exception.PackageVersionNotFoundException missing) { state = "MISSING"; }
        return new SectionQuestionDraftDetail(draft, changed, state);
    }

    @Transactional
    public AiDraft removeItem(UUID author, UUID id, UUID itemId, int revision) {
        AiDraft draft = readyForUpdate(author, id);
        ObjectNode result = result(draft);
        requireRevision(result, revision);
        ArrayNode items = (ArrayNode) result.path("items");
        int index = findItem(items, itemId);
        if (index < 0) throw new AiDraftNotFoundException(itemId);
        items.remove(index);
        result.put("revision", revision + 1);
        return saveResult(draft, result);
    }

    @Transactional
    public AiDraft editItem(UUID author, UUID id, UUID itemId, int revision, JsonNode patch) {
        AiDraft draft = readyForUpdate(author, id);
        ObjectNode result = result(draft);
        requireRevision(result, revision);
        ArrayNode items = (ArrayNode) result.path("items");
        int index = findItem(items, itemId);
        if (index < 0) throw new AiDraftNotFoundException(itemId);
        ObjectNode stored = (ObjectNode) items.get(index);
        ObjectNode candidate = QuestionDraftItems.toCandidate(stored);
        applyPatch(candidate, patch);
        RequestSectionQuestionDraftUseCase.StoredRequest request = request(draft);
        String source = request.source().text();
        List<String> otherStems = new ArrayList<>();
        JsonNode currentItems = result.path("items");
        for (int i = 0; i < currentItems.size(); i++) if (i != index) otherStems.add(currentItems.get(i).path("stem").asText());
        String reason = SectionQuestionChecks.reason(candidate, request.questionType(), source, currentSectionStems(request), otherStems,
                rules.duplicateThreshold(), rules.sameAnswerThreshold(), rules.maxAnswerWords(),
                rules.maxCopiedWords(), rules.maxStemChars());
        if (reason != null) throw sectionError(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_DRAFT_ITEM",
                "Câu hỏi chưa hợp lệ.", Map.of("reason", cleanReason(reason)));
        copyCandidate(stored, candidate);
        result.put("revision", revision + 1);
        return saveResult(draft, result);
    }

    @Transactional
    public AiDraft reject(UUID author, UUID id, String note) {
        AiDraft draft = readyForUpdate(author, id);
        draft.reject(author, note, clock.instant());
        return drafts.save(draft);
    }

    @Transactional
    public AiDraft approve(UUID author, UUID id, List<UUID> itemIds, int revision, boolean acceptSourceChange) {
        AiDraft snapshot = owned(author, id);
        RequestSectionQuestionDraftUseCase.StoredRequest request = request(snapshot);
        ContentPackage pkg = packages.findByIdForUpdate(request.packageId())
                .orElseThrow(() -> new com.ieltspath.content.domain.exception.ContentPackageNotFoundException(request.packageId()));
        AiDraft draft = drafts.findByIdForUpdate(id).filter(d -> d.getRequestedBy().equals(author))
                .orElseThrow(() -> new AiDraftNotFoundException(id));
        if (draft.getKind() != DraftKind.SECTION_QUESTIONS) throw missing(id);
        ObjectNode result = result(draft);
        if (draft.getStatus() == DraftStatus.APPROVED) return alreadyApproved(draft, result, itemIds);
        if (draft.getStatus() != DraftStatus.READY) throw sectionError(HttpStatus.CONFLICT, "DRAFT_NOT_READY", "Bản nháp chưa sẵn sàng để duyệt.", Map.of());
        requireRevision(result, revision);
        if (itemIds == null || itemIds.isEmpty() || new HashSet<>(itemIds).size() != itemIds.size()) {
            throw new IllegalArgumentException("Cần chọn ít nhất một câu hỏi khác nhau.");
        }
        var version = pkg.requireVersion(request.versionId());
        if (version.getStatus() != PublicationStatus.DRAFT) throw sectionError(HttpStatus.BAD_REQUEST, "VERSION_NOT_DRAFT", "Phiên bản đề không còn là bản nháp.", Map.of());
        ContentSection section;
        try { section = pkg.requireSectionInVersion(request.versionId(), request.sectionId()); }
        catch (RuntimeException removed) { throw new com.ieltspath.content.domain.exception.ContentSectionNotFoundException(request.sectionId()); }
        if (section.getSkill() != request.skill()) throw sectionError(HttpStatus.CONFLICT, "SECTION_CHANGED", "Kỹ năng của phần đã thay đổi.", Map.of());
        PackageVersionContentResult latest = content.packageVersionContent(request.versionId())
                .orElseThrow(() -> new com.ieltspath.content.domain.exception.PackageVersionNotFoundException(request.versionId()));
        PackageVersionContentResult.Section latestSection = latest.sections().stream().filter(s -> s.sectionId().equals(section.getId())).findFirst().orElse(null);
        String currentSource = latestSection == null ? null : (request.skill().name().equals("LISTENING")
                ? latestSection.audio() == null ? null : latestSection.audio().transcript() : latestSection.passage());
        String normalized = currentSource == null ? null : currentSource.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (normalized == null || normalized.isBlank()) throw sectionError(HttpStatus.CONFLICT, "SECTION_SOURCE_CHANGED", "Nguồn của phần đã bị gỡ.", Map.of());
        boolean sourceChanged = !sha256(normalized).equals(request.source().sha256());
        if (sourceChanged && !acceptSourceChange) throw sectionError(HttpStatus.CONFLICT, "SECTION_SOURCE_CHANGED", "Nguồn của phần đã thay đổi. Hãy xác nhận trước khi duyệt.", Map.of("sourceChanged", true));
        ArrayNode items = (ArrayNode) result.path("items");
        Map<UUID, JsonNode> byId = new LinkedHashMap<>();
        for (JsonNode item : items) byId.put(UUID.fromString(item.path("itemId").asText()), item);
        if (!byId.keySet().containsAll(itemIds)) throw new IllegalArgumentException("Có câu được chọn không còn trong bản nháp.");
        if (result.path("approved").isArray() && result.path("approved").size() > 0) return alreadyApproved(draft, result, itemIds);
        List<String> currentStems = latestSection.items().stream().map(PackageVersionContentResult.Item::stem).toList();
        List<Map<String, Object>> duplicateItems = new ArrayList<>();
        List<Map<String, Object>> invalidItems = new ArrayList<>();
        List<String> selectedStems = new ArrayList<>();
        for (UUID itemId : itemIds) {
            JsonNode item = byId.get(itemId);
            JsonNode candidate = QuestionDraftItems.toCandidate(item);
            String reason = SectionQuestionChecks.reason(candidate, request.questionType(), normalized,
                    currentStems, selectedStems, rules.duplicateThreshold(), rules.sameAnswerThreshold(), rules.maxAnswerWords(),
                    rules.maxCopiedWords(), rules.maxStemChars());
            if (reason != null && reason.startsWith("DUPLICATE_IN_SECTION:")) duplicateItems.add(Map.of("itemId", itemId, "with", Integer.parseInt(reason.substring(reason.indexOf(':') + 1))));
            else if (reason != null) invalidItems.add(Map.of("itemId", itemId, "reason", reason));
            else selectedStems.add(item.path("stem").asText());
        }
        if (!duplicateItems.isEmpty()) throw sectionError(HttpStatus.CONFLICT, "DRAFT_ITEMS_DUPLICATED", "Một số câu bị trùng với câu đang có trong phần.", Map.of("items", duplicateItems));
        if (!invalidItems.isEmpty()) throw sectionError(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_DRAFT_ITEM", "Một số câu hỏi chưa hợp lệ.", Map.of("items", invalidItems));
        ArrayNode approved = json.createArrayNode();
        for (int order = 0; order < items.size(); order++) {
            JsonNode item = items.get(order);
            UUID itemId = UUID.fromString(item.path("itemId").asText());
            if (!itemIds.contains(itemId)) continue;
            QuestionResult question = createQuestion.execute(new CreateQuestionCommand(request.questionType(), request.skill(), null,
                    purpose(request.purpose()), "ai-" + id + "-" + itemId));
            addVersion.execute(new AddQuestionVersionCommand(question.id(), 1, item.path("stem").asText(), options(item),
                    item.path("answerSpec").toString(), item.path("explanation").asText(), null, QuestionDifficulty.MEDIUM, null));
            entityManager.flush();
            Question saved = questions.findById(question.id()).orElseThrow();
            UUID versionId = saved.getVersions().getLast().getId();
            var attached = addToSection.execute(pkg.getId(), request.versionId(), section.getId(), versionId, BigDecimal.ONE);
            approved.add(json.createObjectNode().put("itemId", itemId.toString()).put("questionId", question.id().toString())
                    .put("questionVersionId", versionId.toString()).put("sectionQuestionId", attached.id().toString()));
        }
        result.set("approved", approved);
        draft.replaceResult(write(result));
        draft.approve(author, clock.instant());
        return drafts.save(draft);
    }

    private AiDraft alreadyApproved(AiDraft draft, ObjectNode result, List<UUID> itemIds) {
        Set<UUID> previously = new HashSet<>();
        result.path("approved").forEach(item -> previously.add(UUID.fromString(item.path("itemId").asText())));
        if (itemIds != null && previously.equals(new HashSet<>(itemIds))) return draft;
        throw sectionError(HttpStatus.CONFLICT, "DRAFT_ALREADY_REVIEWED", "Bản nháp đã được duyệt với danh sách câu khác.", Map.of());
    }
    private AiDraft owned(UUID author, UUID id) { return drafts.findById(id).filter(d -> d.getRequestedBy().equals(author) && d.getKind() == DraftKind.SECTION_QUESTIONS).orElseThrow(() -> missing(id)); }
    private AiDraft readyForUpdate(UUID author, UUID id) {
        AiDraft draft = drafts.findByIdForUpdate(id).filter(d -> d.getRequestedBy().equals(author) && d.getKind() == DraftKind.SECTION_QUESTIONS).orElseThrow(() -> missing(id));
        if (draft.getStatus() != DraftStatus.READY) throw sectionError(HttpStatus.CONFLICT, "DRAFT_NOT_READY", "Bản nháp chưa sẵn sàng để sửa.", Map.of());
        return draft;
    }
    private static AiDraftNotFoundException missing(UUID id) { return new AiDraftNotFoundException(id); }
    private ObjectNode result(AiDraft draft) { try { return (ObjectNode) json.readTree(draft.getResultJson()); } catch (Exception e) { throw sectionError(HttpStatus.CONFLICT, "INVALID_DRAFT", "Không đọc được bản nháp.", Map.of()); } }
    private void requireRevision(ObjectNode result, int revision) { int actual = result.path("revision").asInt(0); if (actual != revision) throw sectionError(HttpStatus.CONFLICT, "DRAFT_CHANGED", "Bản nháp đã được thay đổi ở nơi khác.", Map.of("revision", actual)); }
    private int findItem(ArrayNode items, UUID id) { for (int i=0;i<items.size();i++) if (id.toString().equals(items.get(i).path("itemId").asText())) return i; return -1; }
    private AiDraft saveResult(AiDraft draft, ObjectNode result) { draft.replaceResult(write(result)); return drafts.save(draft); }
    private String write(JsonNode node) { try { return json.writeValueAsString(node); } catch (Exception e) { throw new IllegalStateException("Không lưu được bản nháp", e); } }
    private RequestSectionQuestionDraftUseCase.StoredRequest request(AiDraft draft) { try { return json.readValue(draft.getRequestJson(), RequestSectionQuestionDraftUseCase.StoredRequest.class); } catch (Exception e) { throw sectionError(HttpStatus.CONFLICT, "INVALID_DRAFT", "Không đọc được yêu cầu đã lưu.", Map.of()); } }
    private static SectionDraftException sectionError(HttpStatus status, String code, String message, Map<String,Object> details) { return new SectionDraftException(status, code, message, details); }
    private static String cleanReason(String reason) { int colon=reason.indexOf(':'); return colon<0?reason:reason.substring(0,colon); }
    private QuestionPurpose purpose(String purpose) { return "PLACEMENT".equals(purpose)?QuestionPurpose.PLACEMENT:"PRACTICE".equals(purpose)?QuestionPurpose.LEARNING:QuestionPurpose.EXAM; }
    private List<QuestionOptionPayload> options(JsonNode item) { List<QuestionOptionPayload> out=new ArrayList<>(); for (JsonNode option:item.path("options")) out.add(new QuestionOptionPayload(option.path("optionKey").asText(),option.path("content").asText(),option.path("sortOrder").asInt())); return out; }
    private void applyPatch(ObjectNode candidate, JsonNode patch) {
        if (patch.has("stem")) candidate.put("stem", patch.path("stem").asText());
        if (patch.has("explanation")) candidate.put("explanation", patch.path("explanation").asText());
        if (patch.has("correct")) candidate.put("correct", patch.path("correct").asText());
        if (patch.has("accepted")) candidate.set("accepted", patch.path("accepted").deepCopy());
        if (patch.has("options")) {
            ArrayNode options = json.createArrayNode();
            patch.path("options").forEach(option -> options.add(json.createObjectNode()
                    .put("optionKey", option.path("optionKey").asText()).put("content", option.path("content").asText())));
            candidate.set("options", options);
        }
    }
    private void copyCandidate(ObjectNode stored, ObjectNode candidate) {
        stored.put("stem", candidate.path("stem").asText()); stored.put("explanation", candidate.path("explanation").asText());
        if (candidate.has("options")) {
            ArrayNode options = json.createArrayNode(); int order = 1;
            for (JsonNode option : candidate.path("options")) options.add(json.createObjectNode()
                    .put("optionKey", option.path("optionKey").asText()).put("content", option.path("content").asText())
                    .put("sortOrder", order++));
            stored.set("options", options);
        }
        ObjectNode answer = (ObjectNode) stored.path("answerSpec");
        if (candidate.has("correct")) answer.put("correct", candidate.path("correct").asText());
        if (candidate.has("accepted")) answer.set("accepted", candidate.path("accepted").deepCopy());
        if (candidate.has("distractors")) answer.set("distractors", candidate.path("distractors").deepCopy());
    }
    private String sha256(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch(Exception e){ throw new IllegalStateException(e); } }
    private List<String> currentSectionStems(RequestSectionQuestionDraftUseCase.StoredRequest request) {
        return content.packageVersionContent(request.versionId()).stream().flatMap(version -> version.sections().stream())
                .filter(section -> section.sectionId().equals(request.sectionId())).flatMap(section -> section.items().stream())
                .map(PackageVersionContentResult.Item::stem).toList();
    }
}
