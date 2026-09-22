package com.group01.content.application.usecase;

import com.group01.content.application.command.CreateContentAssetCommand;
import com.group01.content.application.result.ContentAssetResult;
import com.group01.content.domain.aggregate.ContentAsset;
import com.group01.content.domain.repository.ContentAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateContentAssetUseCase {

    private final ContentAssetRepository contentAssetRepository;

    public CreateContentAssetUseCase(ContentAssetRepository contentAssetRepository) {
        this.contentAssetRepository = contentAssetRepository;
    }

    public ContentAssetResult execute(CreateContentAssetCommand command) {
        ContentAsset asset = ContentAsset.create(
                command.assetType(),
                command.textContent(),
                command.mediaReference(),
                command.durationSeconds(),
                command.checksum()
        );

        ContentAsset saved = contentAssetRepository.save(asset);
        return new ContentAssetResult(
                saved.getId(),
                saved.getAssetType(),
                saved.getTextContent(),
                saved.getMediaReference(),
                saved.getDurationSeconds(),
                saved.getChecksum(),
                saved.getValidationStatus(),
                saved.getCreatedAt()
        );
    }
}

