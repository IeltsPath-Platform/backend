package com.group01.content.application.usecase;

import com.group01.content.application.result.ContentPackageResult;
import com.group01.content.domain.aggregate.ContentPackage;
import com.group01.content.domain.repository.ContentPackageRepository;
import com.group01.content.domain.vo.AccessLevel;
import com.group01.content.domain.vo.PublicationStatus;
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

    public List<ContentPackageResult> execute(AccessLevel accessLevel, PublicationStatus status) {
        List<ContentPackage> packages = contentPackageRepository.findAll(accessLevel, status);
        return packages.stream()
                .map(pkg -> new ContentPackageResult(
                        pkg.getId(),
                        pkg.getCode(),
                        pkg.getTitle(),
                        pkg.getPackageType(),
                        pkg.getAccessLevel(),
                        pkg.getStatus(),
                        pkg.getCurrentPublishedVersionId(),
                        pkg.getCreatedAt(),
                        pkg.getUpdatedAt()
                ))
                .toList();
    }
}

