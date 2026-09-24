package com.group01.content.application.usecase;

import com.group01.content.application.result.ContentPackageDetailResult;
import com.group01.content.application.result.ContentSectionResult;
import com.group01.content.application.result.SectionQuestionResult;
import com.group01.content.domain.aggregate.ContentPackage;
import com.group01.content.domain.entity.ContentPackageVersion;
import com.group01.content.domain.exception.ContentPackageNotFoundException;
import com.group01.content.domain.repository.ContentPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetContentPackageDetailUseCase {

    private final ContentPackageRepository contentPackageRepository;

    public GetContentPackageDetailUseCase(ContentPackageRepository contentPackageRepository) {
        this.contentPackageRepository = contentPackageRepository;
    }

    public ContentPackageDetailResult execute(UUID id) {
        ContentPackage pkg = contentPackageRepository.findById(id)
                .orElseThrow(() -> new ContentPackageNotFoundException(id));

        // Find current published version, or the latest version
        ContentPackageVersion activeVersion = null;
        if (pkg.getCurrentPublishedVersionId() != null) {
            for (ContentPackageVersion v : pkg.getVersions()) {
                if (v.getId().equals(pkg.getCurrentPublishedVersionId())) {
                    activeVersion = v;
                    break;
                }
            }
        }
        if (activeVersion == null && !pkg.getVersions().isEmpty()) {
            activeVersion = pkg.getVersions().get(pkg.getVersions().size() - 1);
        }

        List<ContentSectionResult> sectionResults = new ArrayList<>();
        int versionNumber = 0;
        String rulesJson = "{}";
        java.time.Instant publishedAt = null;
        UUID publishedBy = null;

        if (activeVersion != null) {
            versionNumber = activeVersion.getVersionNumber();
            rulesJson = activeVersion.getRulesJson();
            publishedAt = activeVersion.getPublishedAt();
            publishedBy = activeVersion.getPublishedBy();

            for (var sec : activeVersion.getSections()) {
                List<SectionQuestionResult> qResults = new ArrayList<>();
                for (var q : sec.getQuestions()) {
                    qResults.add(new SectionQuestionResult(
                            q.getId(),
                            q.getSectionId(),
                            q.getQuestionVersionId(),
                            q.getSortOrder(),
                            q.getMaxScore(),
                            q.getCreatedAt()
                    ));
                }
                sectionResults.add(new ContentSectionResult(
                        sec.getId(),
                        sec.getPackageVersionId(),
                        sec.getTitle(),
                        sec.getSkill(),
                        sec.getSortOrder(),
                        sec.getTimeLimitSeconds(),
                        sec.getInstructions(),
                        sec.getCreatedAt(),
                        sec.getUpdatedAt(),
                        qResults
                ));
            }
        }

        return new ContentPackageDetailResult(
                pkg.getId(),
                pkg.getCode(),
                pkg.getTitle(),
                pkg.getPackageType(),
                pkg.getRequiredFeatureKey(),
                pkg.getStatus(),
                pkg.getCurrentPublishedVersionId(),
                versionNumber,
                rulesJson,
                publishedAt,
                publishedBy,
                pkg.getCreatedAt(),
                pkg.getUpdatedAt(),
                sectionResults
        );
    }
}
