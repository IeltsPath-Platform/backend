package com.group01.content.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.group01.content.application.command.PublishContentPackageCommand;
import com.group01.content.application.port.LearningContentReader;
import com.group01.content.application.result.ContentPackageResult;
import com.group01.content.domain.exception.InvalidPackageLessonException;
import com.group01.content.domain.aggregate.ContentPackage;
import com.group01.content.domain.entity.ContentPackageVersion;
import com.group01.content.domain.exception.ContentPackageNotFoundException;
import com.group01.content.domain.exception.InvalidContentStateException;
import com.group01.content.domain.exception.QuestionAlreadyUsedException;
import com.group01.content.domain.exception.QuestionPurposeMismatchException;
import com.group01.content.domain.repository.ContentPackageRepository;
import com.group01.content.domain.vo.QuestionUsageConflict;
import com.group01.content.domain.vo.QuestionPurpose;
import com.group01.content.domain.vo.PackageType;
import com.group01.content.domain.vo.QuestionType;
import com.group01.content.domain.vo.Skill;
import com.group01.content.domain.vo.LessonBlockKind;
import com.group01.content.application.result.PackageQuestionSpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class PublishContentPackageUseCase {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private final ContentPackageRepository contentPackageRepository;
    private final LearningContentReader lessons;

    public PublishContentPackageUseCase(ContentPackageRepository contentPackageRepository,
                                        LearningContentReader lessons) {
        this.contentPackageRepository = contentPackageRepository;
        this.lessons = lessons;
    }

    public ContentPackageResult execute(PublishContentPackageCommand command) {
        ContentPackage pkg = contentPackageRepository.findById(command.packageId())
                .orElseThrow(() -> new ContentPackageNotFoundException(command.packageId()));

        ContentPackageVersion targetVersion = null;
        for (ContentPackageVersion v : pkg.getVersions()) {
            if (v.getId().equals(command.versionId())) {
                targetVersion = v;
                break;
            }
        }

        if (targetVersion == null) {
            throw new InvalidContentStateException("Version " + command.versionId() + " does not belong to package " + command.packageId());
        }

        // A lesson's Practice measures only skills taught by that lesson.
        if (pkg.getLessonId() != null
                && lessons.packageVersionLeavesLessonSkills(targetVersion.getId(), pkg.getLessonId())) {
            throw new InvalidPackageLessonException("Every question of a lesson's practice set must belong to the "
                    + "lesson's taught skills");
        }

        switch (pkg.getPackageType()) {
            case PRACTICE_SET, TOPIC_TEST, COURSE_TEST, MOCK_TEST, PLACEMENT_TEST -> {
                lessons.lockQuestionsForPublishing(targetVersion.getId());
                List<QuestionUsageConflict> conflicts = lessons.questionsUsedElsewhere(targetVersion.getId());
                if (!conflicts.isEmpty()) {
                    throw new QuestionAlreadyUsedException(conflicts);
                }
                QuestionPurpose requiredPurpose = switch (pkg.getPackageType()) {
                    case MOCK_TEST, PLACEMENT_TEST -> QuestionPurpose.EXAM;
                    default -> QuestionPurpose.LEARNING;
                };
                List<UUID> wrongPurpose = lessons.questionsWithWrongPurpose(targetVersion.getId(), requiredPurpose);
                if (!wrongPurpose.isEmpty()) {
                    throw new QuestionPurposeMismatchException(requiredPurpose, wrongPurpose);
                }
            }
            default -> { }
        }

        if (pkg.getPackageType() == PackageType.PRACTICE_SET || pkg.getPackageType() == PackageType.TOPIC_TEST
                || pkg.getPackageType() == PackageType.COURSE_TEST) {
            var questions = lessons.packageQuestionSpecs(targetVersion.getId());
            for (var question : questions) {
                JsonNode spec = parseSpec(question.answerSpecJson());
                if ((question.questionType() == QuestionType.ESSAY
                        || LessonBlockKind.ESSAY_SPEC_TYPE.equals(spec.path("type").asText())) && !validEssay(question, spec)) {
                    throw new InvalidContentStateException("Writing essays require passBand from 0 to 9 in steps of 0.5");
                }
            }
            if (pkg.getPackageType() == PackageType.COURSE_TEST
                    && (questions.isEmpty() || questions.stream().anyMatch(question -> !gradableCourseQuestion(question)))) {
                throw new InvalidContentStateException("COURSE_TEST requires gradable Reading or Listening questions, or Writing essays with passBand");
            }
        }

        targetVersion.publish(command.publishedBy());
        pkg.publishVersion(targetVersion.getId());

        return ContentPackageResult.of(contentPackageRepository.save(pkg));
    }

    private static JsonNode parseSpec(String raw) {
        try {
            JsonNode spec = raw == null ? null : JSON.readTree(raw);
            return spec == null ? JSON.nullNode() : spec;
        } catch (JsonProcessingException ex) {
            return JSON.nullNode();
        }
    }

    private static boolean validEssay(PackageQuestionSpec question, JsonNode spec) {
        if (question.skill() != Skill.WRITING || question.questionType() != QuestionType.ESSAY
                || !LessonBlockKind.ESSAY_SPEC_TYPE.equals(spec.path("type").asText())
                || !spec.path("passBand").isNumber()) return false;
        var band = spec.path("passBand").decimalValue();
        return band.signum() >= 0 && band.compareTo(java.math.BigDecimal.valueOf(9)) <= 0
                && band.remainder(new java.math.BigDecimal("0.5")).signum() == 0;
    }

    private static boolean gradableCourseQuestion(PackageQuestionSpec question) {
        if (question.skill() == Skill.WRITING) return validEssay(question, parseSpec(question.answerSpecJson()));
        return (question.skill() == Skill.READING || question.skill() == Skill.LISTENING)
                && question.questionType() != QuestionType.ESSAY && question.questionType() != QuestionType.SPEAKING
                && autoGradable(question.answerSpecJson());
    }

    private static boolean autoGradable(String answerSpecJson) {
        if (answerSpecJson == null) return false;
        try {
            JsonNode spec = JSON.readTree(answerSpecJson);
            if (spec == null || !spec.isObject()) return false;
            String type = spec.has("type") ? spec.path("type").asText() : "CHOICE";
            if ("CHOICE".equals(type)) {
                JsonNode correct = spec.path("correct");
                return correct.isTextual() && !correct.textValue().isBlank();
            }
            if ("FILL".equals(type)) {
                JsonNode accepted = spec.path("accepted");
                if (!accepted.isArray() || accepted.isEmpty()) return false;
                for (JsonNode value : accepted) {
                    if (!value.isTextual() || WHITESPACE.matcher(value.textValue()).replaceAll(" ").trim().isEmpty()) return false;
                }
                return true;
            }
            return false;
        } catch (JsonProcessingException ex) {
            return false;
        }
    }
}
