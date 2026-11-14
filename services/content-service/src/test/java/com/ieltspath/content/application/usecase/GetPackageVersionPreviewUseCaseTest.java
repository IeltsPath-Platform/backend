package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.PackageVersionContentResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
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

class GetPackageVersionPreviewUseCaseTest {
    private final ContentPackageRepository packages = mock(ContentPackageRepository.class);
    private final LearningContentReader reader = mock(LearningContentReader.class);
    private final GetPackageVersionPreviewUseCase useCase = new GetPackageVersionPreviewUseCase(packages, reader,
            new MediaReferencePolicy("https://media.example.test/ieltspath"));
    private final ContentPackage pkg = ContentPackage.create("MOCK-PREVIEW", "Mock", PackageType.MOCK_TEST, null);
    private final ContentPackageVersion draft = ContentPackageVersion.create(pkg.getId(), 1, "{}");
    private final UUID questionVersion = UUID.randomUUID();

    GetPackageVersionPreviewUseCaseTest() {
        pkg.addVersion(draft);
        when(packages.findById(pkg.getId())).thenReturn(Optional.of(pkg));
    }

    private PackageVersionContentResult content(UUID owner) {
        var item = new PackageVersionContentResult.Item(questionVersion, 1, "Stem", "[]",
                "{\"type\":\"CHOICE\",\"correct\":\"A\"}", "Because A.", "Read again.", 1.0, List.of());
        var section = new PackageVersionContentResult.Section(UUID.randomUUID(), "Part 1", Skill.LISTENING, null, 1, null,
                new PackageVersionContentResult.SectionAudio(UUID.randomUUID(), "listening/demo/ls1.mp3", null, 60, "Transcript"),
                List.of(item));
        return new PackageVersionContentResult(draft.getId(), owner, PackageType.MOCK_TEST, null, "{}", List.of(section));
    }

    @Test
    void aVersionThatBelongsToAnotherPackageIsRefused() {
        when(reader.packageVersionContent(draft.getId())).thenReturn(Optional.of(content(UUID.randomUUID())));
        assertThatThrownBy(() -> useCase.execute(pkg.getId(), draft.getId()))
                .isInstanceOf(InvalidContentStateException.class)
                .hasMessage("Version does not belong to requested package");
    }

    @Test
    void answersAreReturnedApartFromTheQuestions() {
        when(reader.packageVersionContent(draft.getId())).thenReturn(Optional.of(content(pkg.getId())));

        var preview = useCase.execute(pkg.getId(), draft.getId());

        var question = preview.sections().getFirst().items().getFirst();
        assertThat(question.questionVersionId()).isEqualTo(questionVersion);
        assertThat(question.stem()).isEqualTo("Stem");
        var answers = preview.answers().getFirst();
        assertThat(answers.audioTranscript()).isEqualTo("Transcript");
        assertThat(answers.items().getFirst().answerSpecJson()).contains("\"correct\":\"A\"");
        assertThat(answers.items().getFirst().explanation()).isEqualTo("Because A.");
        assertThat(preview.sections().getFirst().audio().mediaUrl())
                .isEqualTo("https://media.example.test/ieltspath/listening/demo/ls1.mp3");
    }
}
