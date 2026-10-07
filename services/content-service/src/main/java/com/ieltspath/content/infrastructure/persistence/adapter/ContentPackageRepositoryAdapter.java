package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.infrastructure.persistence.mapper.ContentPackagePersistenceMapper;
import com.ieltspath.content.infrastructure.persistence.repository.ContentPackageJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ContentPackageRepositoryAdapter implements ContentPackageRepository {

    private final ContentPackageJpaRepository contentPackageJpaRepository;
    private final ContentPackagePersistenceMapper mapper;

    public ContentPackageRepositoryAdapter(ContentPackageJpaRepository contentPackageJpaRepository,
                                           ContentPackagePersistenceMapper mapper) {
        this.contentPackageJpaRepository = contentPackageJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ContentPackage save(ContentPackage pkg) {
        var entity = mapper.toEntity(pkg);
        var saved = contentPackageJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ContentPackage> findById(UUID id) {
        return contentPackageJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ContentPackage> findByVersionId(UUID versionId) {
        return contentPackageJpaRepository.findDistinctByVersions_Id(versionId).map(mapper::toDomain);
    }

    @Override
    public Optional<ContentPackage> findBySectionId(UUID sectionId) {
        return contentPackageJpaRepository.findDistinctByVersions_Sections_Id(sectionId).map(mapper::toDomain);
    }

    @Override
    public Optional<ContentPackage> findByCode(String code) {
        return contentPackageJpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<ContentPackage> findAll(Boolean featureRequired, PublicationStatus status) {
        return contentPackageJpaRepository.findAllFiltered(featureRequired, status).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return contentPackageJpaRepository.existsByCode(code);
    }
}
