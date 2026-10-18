package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.PackageVersionContentResult;
import com.ieltspath.content.application.result.PackageVersionPreviewResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.exception.PackageVersionNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.MediaReferencePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetPackageVersionPreviewUseCase {
    private final ContentPackageRepository packages;
    private final LearningContentReader contentReader;
    private final MediaReferencePolicy mediaPolicy;

    public GetPackageVersionPreviewUseCase(ContentPackageRepository packages, LearningContentReader contentReader,
                                           MediaReferencePolicy mediaPolicy) {
        this.packages = packages;
        this.contentReader = contentReader;
        this.mediaPolicy = mediaPolicy;
    }

    public PackageVersionPreviewResult execute(UUID packageId, UUID versionId) {
        ContentPackage pkg = packages.findById(packageId)
                .orElseThrow(() -> new ContentPackageNotFoundException(packageId));
        ContentPackageVersion target = pkg.getVersions().stream().filter(v -> v.getId().equals(versionId))
                .findFirst().orElseThrow(() -> new InvalidContentStateException(
                        "Version " + versionId + " does not belong to package " + packageId));
        PackageVersionContentResult source = contentReader.packageVersionContent(versionId)
                .orElseThrow(() -> new PackageVersionNotFoundException(versionId));
        if (!packageId.equals(source.packageId())) {
            throw new InvalidContentStateException("Version does not belong to requested package");
        }

        List<PackageVersionPreviewResult.Section> sections = source.sections().stream().map(section ->
                new PackageVersionPreviewResult.Section(section.sectionId(), section.title(), section.skill(),
                        section.instructions(), section.sortOrder(), section.passage(),
                        section.audio() == null ? null : new PackageVersionPreviewResult.Audio(
                                section.audio().assetId(), mediaPolicy.resolve(AssetType.AUDIO,
                                section.audio().mediaReference()), section.audio().durationSeconds()),
                        section.items().stream().map(item -> new PackageVersionPreviewResult.Item(
                                item.questionVersionId(), item.sortOrder(), item.stem(), item.optionsJson(),
                                item.hint(), item.maxScore(), item.knowledgePointMappings())).toList())).toList();
        List<PackageVersionPreviewResult.SectionAnswers> answers = source.sections().stream().map(section ->
                new PackageVersionPreviewResult.SectionAnswers(section.sectionId(),
                        section.audio() == null ? null : section.audio().transcript(),
                        section.items().stream().map(item -> new PackageVersionPreviewResult.ItemAnswer(
                                item.questionVersionId(), item.answerSpecJson(), item.explanation(), item.hint()))
                                .toList())).toList();
        return new PackageVersionPreviewResult(target.getId(), packageId, source.packageType(), source.topicId(),
                source.rulesJson(), sections, answers);
    }
}
