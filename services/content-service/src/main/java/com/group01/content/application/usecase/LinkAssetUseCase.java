package com.group01.content.application.usecase;

import com.group01.content.application.command.LinkAssetCommand;
import com.group01.content.domain.entity.ContentAssetLink;
import com.group01.content.domain.exception.AssetNotFoundException;
import com.group01.content.domain.repository.ContentAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LinkAssetUseCase {

    private final ContentAssetRepository contentAssetRepository;

    public LinkAssetUseCase(ContentAssetRepository contentAssetRepository) {
        this.contentAssetRepository = contentAssetRepository;
    }

    public void execute(LinkAssetCommand command) {
        contentAssetRepository.findById(command.assetId())
                .orElseThrow(() -> new AssetNotFoundException(command.assetId()));

        ContentAssetLink link;
        if (command.sectionId() != null) {
            link = ContentAssetLink.forSection(command.assetId(), command.sectionId(), command.sortOrder());
        } else if (command.questionVersionId() != null) {
            link = ContentAssetLink.forQuestionVersion(command.assetId(), command.questionVersionId(), command.sortOrder());
        } else {
            throw new IllegalArgumentException("Either sectionId or questionVersionId must be provided");
        }

        contentAssetRepository.saveLink(link);
    }
}

