package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.PackageVersionContentResult;
import com.ieltspath.content.domain.exception.PackageVersionNotFoundException;
import com.ieltspath.content.domain.vo.MediaReferencePolicy;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetPackageVersionContentUseCaseTest {
    private final LearningContentReader reader = mock(LearningContentReader.class);
    private final GetPackageVersionContentUseCase useCase = new GetPackageVersionContentUseCase(reader,
            new MediaReferencePolicy("https://media.example.test/ieltspath"));

    @Test
    void aVersionTheReaderDoesNotServeAsPublishedIsNotFound() {
        UUID draft = UUID.randomUUID();
        when(reader.publishedPackageVersion(draft)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.execute(draft)).isInstanceOf(PackageVersionNotFoundException.class);
    }

    @Test
    void audioKeysAreJoinedToTheMediaBaseAndHttpsImagesAreKept() {
        UUID versionId = UUID.randomUUID();
        var item = new PackageVersionContentResult.Item(UUID.randomUUID(), 1, "Label the map", "[]",
                "{\"type\":\"FILL\",\"accepted\":[\"bank\"]}", null, null, 1.0, List.of())
                .withImages(List.of(new PackageVersionContentResult.Image("https://cdn.example.com/map.png", null, "Map")));
        var section = new PackageVersionContentResult.Section(UUID.randomUUID(), "Part 1", Skill.LISTENING, null, 1, null,
                new PackageVersionContentResult.SectionAudio(UUID.randomUUID(), "listening/demo/ls1.mp3", null, 60, "transcript"),
                List.of(item));
        when(reader.publishedPackageVersion(versionId)).thenReturn(Optional.of(new PackageVersionContentResult(versionId,
                UUID.randomUUID(), PackageType.MOCK_TEST, null, "{}", List.of(section))));

        var result = useCase.execute(versionId);

        var served = result.sections().getFirst();
        assertThat(served.audio().mediaUrl()).isEqualTo("https://media.example.test/ieltspath/listening/demo/ls1.mp3");
        assertThat(served.items().getFirst().images().getFirst().mediaUrl()).isEqualTo("https://cdn.example.com/map.png");
    }
}
