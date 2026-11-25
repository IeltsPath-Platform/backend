package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.ReadingPassageResult;
import com.ieltspath.content.application.port.LearnerEntitlement;
import com.ieltspath.content.application.exception.PremiumRequiredException;
import com.ieltspath.content.domain.aggregate.ContentAsset;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.entity.ContentSection;
import com.ieltspath.content.domain.exception.ReadingPassageNotFoundException;
import com.ieltspath.content.domain.repository.ContentAssetRepository;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.AssetValidationStatus;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.Skill;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The passage text of a published Reading section, split into labelled paragraphs for study with the tutor.
 *
 * <p>Published practice sets, lessons and practice tests are readable here. Premium-tagged practice tests require a
 * learner entitlement check; other refusals keep the existing "not found" response.
 */
@Service
public class GetReadingPassageUseCase {

    private static final Set<PackageType> READABLE_PACKAGES = Set.of(
            PackageType.PRACTICE_SET, PackageType.PRACTICE_TEST, PackageType.LESSON);
    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("\\R\\s*\\R");

    private final ContentPackageRepository contentPackageRepository;
    private final ContentAssetRepository contentAssetRepository;
    private final LearnerEntitlement entitlement;

    @Autowired
    public GetReadingPassageUseCase(ContentPackageRepository contentPackageRepository,
                                    ContentAssetRepository contentAssetRepository, LearnerEntitlement entitlement) {
        this.contentPackageRepository = contentPackageRepository;
        this.contentAssetRepository = contentAssetRepository;
        this.entitlement = entitlement;
    }

    GetReadingPassageUseCase(ContentPackageRepository contentPackageRepository,
                             ContentAssetRepository contentAssetRepository) {
        this(contentPackageRepository, contentAssetRepository, () -> false);
    }

    @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
    public ReadingPassageResult execute(UUID sectionId, boolean learner) {
        ContentPackage pkg = contentPackageRepository.findBySectionId(sectionId)
                .filter(found -> found.getStatus() == PublicationStatus.PUBLISHED)
                .filter(found -> READABLE_PACKAGES.contains(found.getPackageType()))
                .orElseThrow(() -> new ReadingPassageNotFoundException(sectionId));
        requirePremium(pkg.getRequiredFeatureKey(), learner);
        ContentSection section = publishedSection(pkg, sectionId);
        if (section == null || section.getSkill() != Skill.READING) {
            throw new ReadingPassageNotFoundException(sectionId);
        }

        List<ReadingPassageResult.Paragraph> paragraphs = new ArrayList<>();
        for (ContentAsset asset : contentAssetRepository.findBySectionId(sectionId)) {
            if (asset.getAssetType() != AssetType.PASSAGE || asset.getTextContent() == null
                    || asset.getValidationStatus() != AssetValidationStatus.VALID) {
                continue;
            }
            for (String part : PARAGRAPH_BREAK.split(asset.getTextContent())) {
                String text = part.strip();
                if (!text.isEmpty()) {
                    paragraphs.add(new ReadingPassageResult.Paragraph(label(paragraphs.size()), text));
                }
            }
        }
        if (paragraphs.isEmpty()) {
            throw new ReadingPassageNotFoundException(sectionId);
        }
        return new ReadingPassageResult(section.getId(), section.getTitle(), section.getInstructions(),
                pkg.getId(), pkg.getTitle(), paragraphs);
    }

    private void requirePremium(String requiredFeatureKey, boolean learner) {
        if (learner && requiredFeatureKey != null && !entitlement.premium()) throw new PremiumRequiredException();
    }

    /** The section when it belongs to the package's currently published version; otherwise {@code null}. */
    private static ContentSection publishedSection(ContentPackage pkg, UUID sectionId) {
        for (ContentPackageVersion version : pkg.getVersions()) {
            if (!version.getId().equals(pkg.getCurrentPublishedVersionId())) {
                continue;
            }
            for (ContentSection section : version.getSections()) {
                if (section.getId().equals(sectionId)) {
                    return section;
                }
            }
        }
        return null;
    }

    /** A, B, ... Z, then numbers, so a very long passage still gets unique labels. */
    static String label(int index) {
        return index < 26 ? String.valueOf((char) ('A' + index)) : String.valueOf(index + 1);
    }
}
