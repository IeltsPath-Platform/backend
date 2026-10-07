package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.PublishContentPackageCommand;
import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.PackageQuestionSpec;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MultiSkillPackagePublishingTest {
    private final LearningContentReader reader = mock(LearningContentReader.class);
    private final ContentPackageRepository repository = mock(ContentPackageRepository.class);
    private final PublishContentPackageUseCase useCase = new PublishContentPackageUseCase(repository, reader);

    @Test
    void courseTestsAcceptListeningAndWritingWithHalfBandThresholds() {
        for (PackageQuestionSpec question : List.of(
                spec(QuestionType.MULTIPLE_CHOICE, Skill.LISTENING, "{\"type\":\"CHOICE\",\"correct\":\"A\"}"),
                spec(QuestionType.ESSAY, Skill.WRITING, "{\"type\":\"ESSAY\",\"task\":\"TASK_2\",\"passBand\":5.5}"))) {
            ContentPackage pkg = draft(PackageType.COURSE_TEST, question);
            assertThat(useCase.execute(command(pkg)).status()).isEqualTo(PublicationStatus.PUBLISHED);
        }
    }

    @Test
    void learningPackagesRejectMissingOutOfRangeAndNonHalfBandEssayThresholds() {
        for (PackageType type : List.of(PackageType.PRACTICE_SET, PackageType.TOPIC_TEST, PackageType.COURSE_TEST)) {
            for (String threshold : List.of("", ",\"passBand\":null", ",\"passBand\":-0.5", ",\"passBand\":9.5", ",\"passBand\":5.2", ",\"passBand\":\"5.5\"")) {
                ContentPackage pkg = draft(type, spec(QuestionType.ESSAY, Skill.WRITING,
                        "{\"type\":\"ESSAY\",\"task\":\"TASK_2\"" + threshold + "}"));
                assertThatThrownBy(() -> useCase.execute(command(pkg)))
                        .as("%s rejects %s", type, threshold).isInstanceOf(InvalidContentStateException.class);
                assertThat(pkg.getStatus()).isEqualTo(PublicationStatus.DRAFT);
            }
        }
        verify(repository, never()).save(any());
    }

    @Test
    void learningPackagesAcceptEssayThresholdsAtBothBounds() {
        for (PackageType type : List.of(PackageType.PRACTICE_SET, PackageType.TOPIC_TEST, PackageType.COURSE_TEST)) {
            for (String threshold : List.of("0", "0.5", "9")) {
                ContentPackage pkg = draft(type, spec(QuestionType.ESSAY, Skill.WRITING,
                        "{\"type\":\"ESSAY\",\"task\":\"TASK_2\",\"passBand\":" + threshold + "}"));
                assertThat(useCase.execute(command(pkg)).status()).isEqualTo(PublicationStatus.PUBLISHED);
            }
        }
    }

    private PackageQuestionSpec spec(QuestionType type, Skill skill, String spec) {
        return new PackageQuestionSpec(UUID.randomUUID(), type, skill, spec);
    }

    private ContentPackage draft(PackageType type, PackageQuestionSpec question) {
        var pkg = new ContentPackage(UUID.randomUUID(), "PUBLISH_TEST", "Publishing", type, null,
                PublicationStatus.DRAFT, null, null, null, List.of(), null,
                type == PackageType.COURSE_TEST ? UUID.randomUUID() : null);
        pkg.addVersion(ContentPackageVersion.create(pkg.getId(), 1, "{}"));
        when(repository.findById(pkg.getId())).thenReturn(Optional.of(pkg));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(reader.packageQuestionSpecs(pkg.getVersions().getFirst().getId())).thenReturn(List.of(question));
        return pkg;
    }

    private PublishContentPackageCommand command(ContentPackage pkg) {
        return new PublishContentPackageCommand(pkg.getId(), pkg.getVersions().getFirst().getId(), UUID.randomUUID());
    }
}
