package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.QuestionPassageReader;
import com.ieltspath.content.application.port.QuestionPassageReader.Passage;
import com.ieltspath.content.application.port.QuestionPassageReader.Source;
import com.ieltspath.content.domain.vo.AccessTier;
import com.ieltspath.content.domain.vo.AssetType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GetQuestionPassagesUseCaseTest {

    private final QuestionPassageReader reader = mock(QuestionPassageReader.class);
    private final MediaUrlResolver urls = mock(MediaUrlResolver.class);
    private final GetQuestionPassagesUseCase useCase = new GetQuestionPassagesUseCase(reader, urls);
    private final UUID version = UUID.randomUUID();

    private static Passage recording(String reference, AccessTier tier) {
        return new Passage(UUID.randomUUID(), AssetType.AUDIO, "Transcript", Source.SECTION, "Section", reference, tier, 60, null);
    }

    @Test
    void givesARecordingAPlayableLinkAndSignsPrivateOnes() {
        Passage premium = recording("clip.mp3", AccessTier.PREMIUM);
        when(reader.passagesOf(version)).thenReturn(List.of(premium));
        when(urls.resolve(AssetType.AUDIO, "clip.mp3", AccessTier.PREMIUM, true)).thenReturn("https://signed.example/clip.mp3?token=x");

        List<Passage> result = useCase.execute(version);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).mediaUrl()).isEqualTo("https://signed.example/clip.mp3?token=x");
        assertThat(result.get(0).text()).isEqualTo("Transcript");
    }

    @Test
    void leavesAReadingPassageAloneWithoutAskingForALink() {
        Passage text = new Passage(UUID.randomUUID(), "A. Passage", Source.DIRECT, null);
        when(reader.passagesOf(version)).thenReturn(List.of(text));

        assertThat(useCase.execute(version)).containsExactly(text);
        verifyNoInteractions(urls);
    }

    @Test
    void aRecordingThatCannotGetALinkKeepsItsTranscriptAndDoesNotHideTheOtherAssets() {
        Passage broken = recording("bad key", AccessTier.PUBLIC);
        Passage text = new Passage(UUID.randomUUID(), "A. Passage", Source.DIRECT, null);
        when(reader.passagesOf(version)).thenReturn(List.of(broken, text));
        when(urls.resolve(eq(AssetType.AUDIO), any(), any(), anyBoolean())).thenThrow(new IllegalArgumentException("bad reference"));

        List<Passage> result = useCase.execute(version);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).mediaUrl()).isNull();
        assertThat(result.get(0).text()).isEqualTo("Transcript");
        assertThat(result.get(1)).isEqualTo(text);
    }
}
