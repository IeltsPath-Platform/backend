package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.NewerQuestionVersionFinder;
import com.ieltspath.content.application.result.AuthorSectionQuestionResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentSection;
import com.ieltspath.content.domain.entity.SectionQuestion;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Moves a question of a draft section to the newest published version of that same question, in place: the row keeps
 * its order, score and group, so a Writing task can take an edited prompt without swapping its question.
 */
@Service
@Transactional
public class UseNewerSectionQuestionVersionUseCase {

    private final ContentPackageRepository packages;
    private final NewerQuestionVersionFinder newerVersions;

    public UseNewerSectionQuestionVersionUseCase(ContentPackageRepository packages, NewerQuestionVersionFinder newerVersions) {
        this.packages = packages;
        this.newerVersions = newerVersions;
    }

    public AuthorSectionQuestionResult execute(UUID packageId, UUID versionId, UUID sectionId, UUID sectionQuestionId) {
        ContentPackage pkg = packages.findByIdForUpdate(packageId)
                .orElseThrow(() -> new ContentPackageNotFoundException(packageId));
        pkg.requireVersion(versionId).requireDraft();
        ContentSection section = pkg.requireSectionInVersion(versionId, sectionId);
        SectionQuestion row = section.requireQuestion(sectionQuestionId);
        UUID newer = newerVersions.newerPublishedVersion(row.getQuestionVersionId())
                .orElseThrow(() -> new InvalidContentStateException("Câu hỏi này chưa có phiên bản mới đã xuất bản"));
        pkg.requireVersion(versionId).requireQuestionNotInVersion(newer);
        row.useVersion(newer);
        packages.save(pkg);
        return new AuthorSectionQuestionResult(row.getId(), newer, row.getSortOrder(), row.getMaxScore());
    }
}
