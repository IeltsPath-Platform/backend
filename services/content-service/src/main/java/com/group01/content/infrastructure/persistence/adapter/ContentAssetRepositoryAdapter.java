package com.group01.content.infrastructure.persistence.adapter;

import com.group01.content.domain.aggregate.ContentAsset;
import com.group01.content.domain.entity.ContentAssetLink;
import com.group01.content.domain.repository.ContentAssetRepository;
import com.group01.content.infrastructure.persistence.mapper.ContentAssetPersistenceMapper;
import com.group01.content.infrastructure.persistence.repository.ContentAssetJpaRepository;
import com.group01.content.infrastructure.persistence.repository.ContentAssetLinkJpaRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ContentAssetRepositoryAdapter implements ContentAssetRepository {

    private final ContentAssetJpaRepository contentAssetJpaRepository;
    private final ContentAssetLinkJpaRepository contentAssetLinkJpaRepository;
    private final ContentAssetPersistenceMapper mapper;

    public ContentAssetRepositoryAdapter(ContentAssetJpaRepository contentAssetJpaRepository,
                                         ContentAssetLinkJpaRepository contentAssetLinkJpaRepository,
                                         ContentAssetPersistenceMapper mapper) {
        this.contentAssetJpaRepository = contentAssetJpaRepository;
        this.contentAssetLinkJpaRepository = contentAssetLinkJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ContentAsset save(ContentAsset asset) {
        var entity = mapper.toEntity(asset);
        var saved = contentAssetJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ContentAsset> findById(UUID id) {
        return contentAssetJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public ContentAssetLink saveLink(ContentAssetLink link) {
        var entity = mapper.toLinkEntity(link);
        var saved = contentAssetLinkJpaRepository.save(entity);
        return mapper.toLinkDomain(saved);
    }

    @Override
    public List<ContentAsset> findBySectionId(UUID sectionId) {
        var links = contentAssetLinkJpaRepository.findBySectionIdOrderBySortOrderAsc(sectionId);
        List<ContentAsset> result = new ArrayList<>();
        for (var link : links) {
            contentAssetJpaRepository.findById(link.getAssetId()).ifPresent(e -> result.add(mapper.toDomain(e)));
        }
        return result;
    }

    @Override
    public List<ContentAsset> findByQuestionVersionId(UUID questionVersionId) {
        var links = contentAssetLinkJpaRepository.findByQuestionVersionIdOrderBySortOrderAsc(questionVersionId);
        List<ContentAsset> result = new ArrayList<>();
        for (var link : links) {
            contentAssetJpaRepository.findById(link.getAssetId()).ifPresent(e -> result.add(mapper.toDomain(e)));
        }
        return result;
    }
}

