package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.command.SectionQuestionDraftRequest;
import com.ieltspath.content.application.exception.SectionDraftException;
import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.PackageVersionContentResult;
import com.ieltspath.content.application.result.SectionQuestionRules;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.entity.ContentSection;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.exception.SectionDraftOpenException;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.Skill;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import org.springframework.http.HttpStatus;

@Service
public class RequestSectionQuestionDraftUseCase {
    private static final Set<PackageType> SUPPORTED_PACKAGES = Set.of(PackageType.MOCK_TEST, PackageType.PLACEMENT_TEST,
            PackageType.PRACTICE_SET, PackageType.TOPIC_TEST, PackageType.COURSE_TEST);
    private final ContentPackageRepository packages;
    private final LearningContentReader content;
    private final AiDraftRepository drafts;
    private final StartAiDraftUseCase start;
    private final ObjectMapper json;
    private final SectionQuestionRules rules;

    public RequestSectionQuestionDraftUseCase(ContentPackageRepository packages, LearningContentReader content,
            AiDraftRepository drafts, StartAiDraftUseCase start, ObjectMapper json, SectionQuestionRules rules) {
        this.packages = packages; this.content = content; this.drafts = drafts; this.start = start;
        this.json = json; this.rules = rules;
    }

    @Transactional
    public AiDraft execute(UUID author, SectionQuestionDraftRequest request) {
        ContentPackage pkg = packages.findByIdForUpdate(request.packageId())
                .orElseThrow(() -> new ContentPackageNotFoundException(request.packageId()));
        ContentPackageVersion version = pkg.requireVersion(request.versionId());
        version.requireDraft();
        ContentSection section = pkg.requireSectionInVersion(request.versionId(), request.sectionId());
        if (!SUPPORTED_PACKAGES.contains(pkg.getPackageType()) || section.getSkill() == null
                || !supported(section.getSkill(), request.questionType())) {
            throw new SectionDraftException(HttpStatus.BAD_REQUEST, "SECTION_NOT_SUPPORTED",
                    "Loại đề, kỹ năng hoặc dạng câu hỏi này chưa được hỗ trợ.", Map.of());
        }
        if (request.count() < 1 || request.count() > rules.maxCount()) {
            throw new IllegalArgumentException("Số câu phải từ 1 đến " + rules.maxCount());
        }
        if (request.instructions() != null && request.instructions().length() > 500) {
            throw new IllegalArgumentException("Hướng dẫn thêm không được dài quá 500 ký tự");
        }
        var existing = drafts.findOpenSectionDraft(author, section.getId());
        if (existing.isPresent()) throw new SectionDraftOpenException(existing.get().getId());
        PackageVersionContentResult versionContent = content.packageVersionContent(version.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên bản đề"));
        PackageVersionContentResult.Section sourceSection = versionContent.sections().stream()
                .filter(candidate -> candidate.sectionId().equals(section.getId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Phần không thuộc phiên bản đề này"));
        String sourceType = section.getSkill() == Skill.LISTENING ? "TRANSCRIPT" : "PASSAGE";
        String source = normalize(section.getSkill() == Skill.LISTENING
                ? sourceSection.audio() == null ? null : sourceSection.audio().transcript()
                : sourceSection.passage());
        if (source == null || source.isBlank()) throw new SectionDraftException(HttpStatus.UNPROCESSABLE_ENTITY,
                "SECTION_SOURCE_REQUIRED", "Phần chưa có nội dung nguồn để tạo câu hỏi.", Map.of());
        if (source.length() > rules.maxSourceChars()) {
            throw new SectionDraftException(HttpStatus.UNPROCESSABLE_ENTITY, "SECTION_SOURCE_TOO_LONG",
                    "Nội dung nguồn vượt quá giới hạn 20.000 ký tự.", Map.of());
        }
        long words = source.split("\\s+").length;
        if (words < rules.minSourceWords()) {
            throw new SectionDraftException(HttpStatus.UNPROCESSABLE_ENTITY, "SECTION_SOURCE_TOO_SHORT",
                    "Nội dung nguồn phải có ít nhất 80 từ.", Map.of());
        }
        String requestJson = write(new StoredRequest(pkg.getId(), version.getId(), section.getId(), request.questionType(),
                request.count(), request.instructions(), section.getSkill(), purpose(pkg.getPackageType()),
                section.getInstructions(), new Source(sourceType, source, sha256(source))));
        return start.execute(author, DraftKind.SECTION_QUESTIONS, requestJson, rules.dailyLimit());
    }

    private static boolean supported(Skill skill, com.ieltspath.content.domain.vo.QuestionType type) {
        if (type == null) return false;
        return skill == Skill.LISTENING
                ? Set.of(com.ieltspath.content.domain.vo.QuestionType.MULTIPLE_CHOICE,
                    com.ieltspath.content.domain.vo.QuestionType.FILL_IN_BLANK,
                    com.ieltspath.content.domain.vo.QuestionType.SHORT_ANSWER).contains(type)
                : skill == Skill.READING && Set.of(com.ieltspath.content.domain.vo.QuestionType.MULTIPLE_CHOICE,
                    com.ieltspath.content.domain.vo.QuestionType.FILL_IN_BLANK,
                    com.ieltspath.content.domain.vo.QuestionType.SHORT_ANSWER,
                    com.ieltspath.content.domain.vo.QuestionType.TRUE_FALSE_NOT_GIVEN).contains(type);
    }
    private static String purpose(PackageType type) {
        return switch (type) {
            case MOCK_TEST -> "EXAM";
            case PLACEMENT_TEST -> "PLACEMENT";
            case PRACTICE_SET -> "PRACTICE";
            case TOPIC_TEST -> "TOPIC_TEST";
            case COURSE_TEST -> "COURSE_TEST";
            default -> throw new IllegalArgumentException("Loại đề không hỗ trợ");
        };
    }
    private static String normalize(String value) {
        return value == null ? null : value.replace("\r\n", "\n").replace('\r', '\n').strip();
    }
    private static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private String write(StoredRequest request) {
        try { return json.writeValueAsString(request); }
        catch (JsonProcessingException invalid) { throw new IllegalStateException("Không lưu được yêu cầu tạo câu hỏi", invalid); }
    }
    public record Source(String type, String text, String sha256) {}
    public record StoredRequest(UUID packageId, UUID versionId, UUID sectionId,
            com.ieltspath.content.domain.vo.QuestionType questionType, int count, String instructions,
            Skill skill, String purpose, String sectionInstructions, Source source) {}
}
