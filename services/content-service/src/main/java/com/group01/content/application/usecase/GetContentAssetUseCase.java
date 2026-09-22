package com.group01.content.application.usecase;

import com.group01.content.application.result.ContentAssetResult;
import com.group01.content.domain.aggregate.ContentAsset;
import com.group01.content.domain.exception.AssetNotFoundException;
import com.group01.content.domain.repository.ContentAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetContentAssetUseCase {

    private final ContentAssetRepository contentAssetRepository;

    public GetContentAssetUseCase(ContentAssetRepository contentAssetRepository) {
        this.contentAssetRepository = contentAssetRepository;
    }

    public ContentAssetResult execute(UUID id) {
        ContentAsset asset = contentAssetRepository.findById(id)
                .orElseThrow(() -> new AssetNotFoundException(id));

        return new ContentAssetResult(
                asset.getId(),
                asset.getAssetType(),
                asset.getTextContent(),
                asset.getMediaReference(),
                asset.getDurationSeconds(),
                asset.getChecksum(),
                asset.getValidationStatus(),
                asset.getCreatedAt()
        );
    }
}

