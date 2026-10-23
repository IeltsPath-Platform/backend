package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.result.AuthorVersionResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CreateDraftPackageVersionUseCase {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ContentPackageRepository contentPackageRepository;

    public CreateDraftPackageVersionUseCase(ContentPackageRepository contentPackageRepository) {
        this.contentPackageRepository = contentPackageRepository;
    }

    /** Locks the package so two requests cannot both see "no draft" and each add one. */
    public AuthorVersionResult execute(UUID packageId, String rulesJson) {
        String rules = rulesJson == null || rulesJson.isBlank() ? "{}" : rulesJson;
        requireJson(rules);

        ContentPackage pkg = contentPackageRepository.findByIdForUpdate(packageId)
                .orElseThrow(() -> new ContentPackageNotFoundException(packageId));
        ContentPackageVersion draft = pkg.createDraftVersion(rules);
        contentPackageRepository.save(pkg);
        return AuthorVersionResult.of(draft);
    }

    private static void requireJson(String rules) {
        try {
            JSON.readTree(rules);
        } catch (JsonProcessingException invalid) {
            throw new IllegalArgumentException("rulesJson must be valid JSON");
        }
    }
}
