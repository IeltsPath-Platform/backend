package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateContentAssetCommand;
import com.ieltspath.content.domain.aggregate.ContentAsset;
import com.ieltspath.content.domain.repository.ContentAssetRepository;
import com.ieltspath.content.domain.vo.AccessTier;
import com.ieltspath.content.domain.vo.AssetType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateContentAssetUseCaseTest {
    @Test
    void savesTheSelectedAccessTierForLessonAudio() {
        ContentAssetRepository repository = mock(ContentAssetRepository.class);
        when(repository.save(any(ContentAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var useCase = new CreateContentAssetUseCase(repository);
        var command = new CreateContentAssetCommand(AssetType.AUDIO, "Transcript", "listening/demo/ls1.mp3",
                45, null, AccessTier.PREMIUM);

        var result = useCase.execute(command);

        var saved = org.mockito.ArgumentCaptor.forClass(ContentAsset.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getAccessTier()).isEqualTo(AccessTier.PREMIUM);
        assertThat(result.accessTier()).isEqualTo(AccessTier.PREMIUM);
    }
}
