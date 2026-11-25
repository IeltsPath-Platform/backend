package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.port.PackageVersionCopier;
import com.ieltspath.content.application.result.AuthorVersionResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Starts a draft from the version learners currently get, so an author edits what exists instead of rebuilding it.
 * A package with no earlier version gets an empty draft.
 */
@Service
@Transactional
public class CreateDraftFromCurrentVersionUseCase {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ContentPackageRepository contentPackageRepository;
    private final PackageVersionCopier copier;

    public CreateDraftFromCurrentVersionUseCase(ContentPackageRepository contentPackageRepository,
                                                PackageVersionCopier copier) {
        this.contentPackageRepository = contentPackageRepository;
        this.copier = copier;
    }

    /** Locks the package so two requests cannot both see "no draft" and each add one. */
    public AuthorVersionResult execute(UUID packageId, String rulesJson) {
        ContentPackage pkg = contentPackageRepository.findByIdForUpdate(packageId)
                .orElseThrow(() -> new ContentPackageNotFoundException(packageId));
        Optional<ContentPackageVersion> source = pkg.copySourceVersion();
        String rules = rulesJson != null && !rulesJson.isBlank() ? rulesJson
                : source.map(ContentPackageVersion::getRulesJson).orElse("{}");
        requireJson(rules);

        ContentPackageVersion draft = pkg.createDraftVersion(rules);
        contentPackageRepository.save(pkg);
        source.ifPresent(version -> copier.copyContent(version.getId(), draft.getId()));
        return AuthorVersionResult.of(draft);
    }

    private static void requireJson(String rules) {
        try {
            JSON.readTree(rules);
        } catch (JsonProcessingException invalid) {
            throw new IllegalArgumentException("Quy tắc của phiên bản phải là JSON hợp lệ");
        }
    }
}
