package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.LinkAssetCommand;
import com.ieltspath.content.domain.aggregate.ContentAsset;
import com.ieltspath.content.domain.exception.AssetNotFoundException;
import com.ieltspath.content.domain.repository.ContentAssetRepository;
import com.ieltspath.content.domain.vo.AssetType;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LinkAssetUseCaseTest {
    private final ContentAssetRepository assets = mock(ContentAssetRepository.class);
    private final LinkAssetUseCase useCase = new LinkAssetUseCase(assets);

    @Test
    void aLinkNeedsAKnownAssetAndATarget() {
        ContentAsset asset = ContentAsset.create(AssetType.AUDIO, null, "listening/demo/ls1.mp3", 60, null);
        when(assets.findById(asset.getId())).thenReturn(Optional.of(asset));
        assertThatThrownBy(() -> useCase.execute(new LinkAssetCommand(asset.getId(), null, null, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Either sectionId or questionVersionId must be provided");
        UUID unknown = UUID.randomUUID();
        when(assets.findById(unknown)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.execute(new LinkAssetCommand(unknown, UUID.randomUUID(), null, 1)))
                .isInstanceOf(AssetNotFoundException.class);
        verify(assets, never()).saveLink(any());
    }
}
