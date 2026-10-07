package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.ContentPackageResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.PublicationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListContentPackagesUseCase {

    private final ContentPackageRepository contentPackageRepository;

    public ListContentPackagesUseCase(ContentPackageRepository contentPackageRepository) {
        this.contentPackageRepository = contentPackageRepository;
    }

    public List<ContentPackageResult> execute(Boolean featureRequired, PublicationStatus status) {
        List<ContentPackage> packages = contentPackageRepository.findAll(featureRequired, status);
        return packages.stream()
                .map(ContentPackageResult::of)
                .toList();
    }
}
