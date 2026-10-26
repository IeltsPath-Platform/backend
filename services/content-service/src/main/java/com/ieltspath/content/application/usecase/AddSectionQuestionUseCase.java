package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.AuthorSectionQuestionResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentSection;
import com.ieltspath.content.domain.entity.SectionQuestion;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.exception.QuestionVersionNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/** Attaches a question version to a draft section. Publication still checks usage elsewhere and the purpose. */
@Service
@Transactional
public class AddSectionQuestionUseCase {

    private final ContentPackageRepository contentPackageRepository;

    public AddSectionQuestionUseCase(ContentPackageRepository contentPackageRepository) {
        this.contentPackageRepository = contentPackageRepository;
    }

    public AuthorSectionQuestionResult execute(UUID packageId, UUID versionId, UUID sectionId,
                                               UUID questionVersionId, BigDecimal maxScore) {
        ContentPackage pkg = contentPackageRepository.findByIdForUpdate(packageId)
                .orElseThrow(() -> new ContentPackageNotFoundException(packageId));
        pkg.requireVersion(versionId).requireDraft();
        ContentSection section = pkg.requireSectionInVersion(versionId, sectionId);
        if (questionVersionId == null) {
            throw new IllegalArgumentException("questionVersionId is required");
        }
        if (!contentPackageRepository.questionVersionExists(questionVersionId)) {
            throw new QuestionVersionNotFoundException(questionVersionId);
        }
        BigDecimal score = maxScore == null ? BigDecimal.ONE : maxScore;
        if (score.signum() <= 0) {
            throw new IllegalArgumentException("maxScore must be positive");
        }
        pkg.requireVersion(versionId).requireQuestionNotInVersion(questionVersionId);

        SectionQuestion question = SectionQuestion.create(section.getId(), questionVersionId,
                section.nextQuestionSortOrder(), score);
        section.addQuestion(question);
        contentPackageRepository.save(pkg);
        return new AuthorSectionQuestionResult(question.getId(), questionVersionId, question.getSortOrder(), score);
    }
}
