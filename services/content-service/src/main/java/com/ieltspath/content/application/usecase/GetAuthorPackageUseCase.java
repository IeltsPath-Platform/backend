package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.AuthorPackageResult;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetAuthorPackageUseCase {

    private final ContentPackageRepository contentPackageRepository;

    public GetAuthorPackageUseCase(ContentPackageRepository contentPackageRepository) {
        this.contentPackageRepository = contentPackageRepository;
    }

    public AuthorPackageResult execute(UUID packageId) {
        return AuthorPackageResult.of(contentPackageRepository.findById(packageId)
                .orElseThrow(() -> new ContentPackageNotFoundException(packageId)));
    }
}
