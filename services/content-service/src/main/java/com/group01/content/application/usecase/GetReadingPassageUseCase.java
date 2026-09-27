package com.group01.content.application.usecase;

import com.group01.content.application.result.ReadingPassageResult;
import com.group01.content.domain.aggregate.ContentAsset;
import com.group01.content.domain.aggregate.ContentPackage;
import com.group01.content.domain.entity.ContentPackageVersion;
import com.group01.content.domain.entity.ContentSection;
import com.group01.content.domain.exception.ReadingPassageNotFoundException;
import com.group01.content.domain.repository.ContentAssetRepository;
import com.group01.content.domain.repository.ContentPackageRepository;
import com.group01.content.domain.vo.AssetType;
import com.group01.content.domain.vo.AssetValidationStatus;
import com.group01.content.domain.vo.PackageType;
import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.domain.vo.Skill;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The passage text of a published Reading section, split into labelled paragraphs for study with the tutor.
 *
 * <p>Only practice sets and lessons are readable here: mock and placement tests stay unseen until the learner sits
 * them. Every refusal is the same "not found", whatever the reason.
 */
@Service
@Transactional(readOnly = true)
public class GetReadingPassageUseCase {

    private static final Set<PackageType> READABLE_PACKAGES = Set.of(PackageType.PRACTICE_SET, PackageType.LESSON);
    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("\\R\\s*\\R");

    private final ContentPackageRepository contentPackageRepository;
    private final ContentAssetRepository contentAssetRepository;

    public GetReadingPassageUseCase(ContentPackageRepository contentPackageRepository,
                                    ContentAssetRepository contentAssetRepository) {
        this.contentPackageRepository = contentPackageRepository;
        this.contentAssetRepository = contentAssetRepository;
    }

    public ReadingPassageResult execute(UUID sectionId) {
        ContentPackage pkg = contentPackageRepository.findBySectionId(sectionId)
                .filter(found -> found.getStatus() == PublicationStatus.PUBLISHED)
                .filter(found -> READABLE_PACKAGES.contains(found.getPackageType()))
                .orElseThrow(() -> new ReadingPassageNotFoundException(sectionId));
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
