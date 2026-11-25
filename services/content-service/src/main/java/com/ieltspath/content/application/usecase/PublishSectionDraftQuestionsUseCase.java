package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.exception.SectionDraftException;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.entity.QuestionVersion;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.exception.QuestionVersionNotPublishableException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.PublicationStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PublishSectionDraftQuestionsUseCase {
    private final ContentPackageRepository packages;
    private final QuestionRepository questions;
    private final PublishQuestionVersionUseCase publisher;
    public PublishSectionDraftQuestionsUseCase(ContentPackageRepository packages, QuestionRepository questions,
                                               PublishQuestionVersionUseCase publisher) {
        this.packages = packages; this.questions = questions; this.publisher = publisher;
    }
    @Transactional
    public int execute(UUID packageId, UUID versionId, UUID sectionId) {
        ContentPackage pkg = packages.findByIdForUpdate(packageId).orElseThrow(() -> new ContentPackageNotFoundException(packageId));
        pkg.requireVersion(versionId).requireDraft();
        var section = pkg.requireSectionInVersion(versionId, sectionId);
        List<UUID> versionIds = section.getQuestions().stream().map(q -> q.getQuestionVersionId()).distinct().toList();
        List<Question> locked = questions.findByVersionIdsForUpdate(versionIds);
        Map<UUID, Question> ownerByVersion = new HashMap<>();
        for (Question question : locked) for (QuestionVersion version : question.getVersions()) ownerByVersion.put(version.getId(), question);
        if (ownerByVersion.keySet().stream().filter(versionIds::contains).count() != versionIds.size()) {
            List<Map<String, Object>> missing = versionIds.stream().filter(id -> !ownerByVersion.containsKey(id))
                    .map(id -> Map.<String, Object>of("questionVersionId", id)).toList();
            throw new SectionDraftException(HttpStatus.UNPROCESSABLE_ENTITY, "QUESTION_VERSION_NOT_PUBLISHABLE",
                    "Một số câu hỏi chưa đủ điều kiện xuất bản.", Map.of("questions", missing));
        }
        List<QuestionVersion> drafts = versionIds.stream().map(ownerByVersion::get).filter(Objects::nonNull)
                .flatMap(question -> question.getVersions().stream().filter(v -> versionIds.contains(v.getId())
                        && v.getStatus() == PublicationStatus.DRAFT).map(v -> v)).toList();
        List<Map<String, Object>> invalid = new ArrayList<>();
        for (QuestionVersion version : drafts) {
            Question question = ownerByVersion.get(version.getId());
            try { publisher.validateForPublishing(question.getQuestionType(), version); }
            catch (QuestionVersionNotPublishableException failure) { invalid.add(Map.of("questionId", question.getId(), "questionVersionId", version.getId())); }
        }
        if (!invalid.isEmpty()) throw new SectionDraftException(HttpStatus.UNPROCESSABLE_ENTITY,
                "QUESTION_VERSION_NOT_PUBLISHABLE", "Một số câu hỏi chưa đủ điều kiện xuất bản.", Map.of("questions", invalid));
        for (QuestionVersion version : drafts) {
            Question question = ownerByVersion.get(version.getId());
            question.publishVersion(version.getId());
            questions.save(question);
        }
        return drafts.size();
    }
}
