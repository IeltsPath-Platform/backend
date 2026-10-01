package com.group01.content.application.usecase;

import com.group01.content.application.command.CreateContentPackageCommand;
import com.group01.content.application.command.SearchPracticeSetsCommand;
import com.group01.content.application.port.LearningContentReader;
import com.group01.content.domain.vo.MediaReferencePolicy;
import com.group01.content.domain.exception.LessonNotFoundException;
import com.group01.content.domain.exception.PackageVersionNotFoundException;
import com.group01.content.domain.exception.TopicNotFoundException;
import com.group01.content.domain.repository.ContentPackageRepository;
import com.group01.content.domain.vo.PackageType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class LearningContentUseCasesTest {
    private final LearningContentReader reader = mock(LearningContentReader.class);

    @Test
    void practiceSetSearchAppliesDefaults() {
        UUID kp = UUID.randomUUID();
        new SearchPracticeSetsUseCase(reader).execute(new SearchPracticeSetsCommand(kp, null, null, null));
        verify(reader).searchPracticeSets(kp, List.of(), 3, 1);
    }

    @Test
    void practiceSetSearchPassesExclusionsAndBounds() {
        UUID kp = UUID.randomUUID();
        List<UUID> exclude = List.of(UUID.randomUUID());
        new SearchPracticeSetsUseCase(reader).execute(new SearchPracticeSetsCommand(kp, exclude, 4, 10));
        verify(reader).searchPracticeSets(kp, exclude, 4, 10);
    }

    @Test
    void practiceSetSearchRejectsInvalidInputWithoutQuerying() {
        SearchPracticeSetsUseCase useCase = new SearchPracticeSetsUseCase(reader);
        UUID kp = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new SearchPracticeSetsCommand(null, null, null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new SearchPracticeSetsCommand(kp, null, null, 11)));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new SearchPracticeSetsCommand(kp, null, 0, null)));
        verify(reader, never()).searchPracticeSets(any(), any(), anyInt(), anyInt());
    }

    @Test
    void topicSequenceUsesTheSearchDefaultForPracticeSets() {
        new GetTopicSequenceUseCase(reader).execute();
        verify(reader).topicSequence(SearchPracticeSetsUseCase.DEFAULT_MIN_QUESTIONS);
    }

    @Test
    void unknownTopicLessonOrVersionIsNotFound() {
        UUID id = UUID.randomUUID();
        when(reader.activeTopicExists(id)).thenReturn(false);
        when(reader.publishedLesson(id)).thenReturn(Optional.empty());
        when(reader.publishedPackageVersion(id)).thenReturn(Optional.empty());

        assertThrows(TopicNotFoundException.class, () -> new GetTopicLessonsUseCase(reader).execute(id));
        assertThrows(TopicNotFoundException.class, () -> new GetTopicTestPackagesUseCase(reader).execute(id));
        assertThrows(LessonNotFoundException.class, () -> new GetLessonContentUseCase(reader, new MediaReferencePolicy("")).execute(id));
        assertThrows(PackageVersionNotFoundException.class,
                () -> new GetPackageVersionContentUseCase(reader, new MediaReferencePolicy("")).execute(id));
        verify(reader, never()).publishedLessons(id);
        verify(reader, never()).publishedTestPackages(id);
    }

    @Test
    void topicTestPackagesCannotBeCreatedThroughTheApi() {
        ContentPackageRepository packages = mock(ContentPackageRepository.class);
        assertThrows(IllegalArgumentException.class, () -> new CreateContentPackageUseCase(packages).execute(
                new CreateContentPackageCommand("X9", "Test", PackageType.TOPIC_TEST, null)));
        verifyNoInteractions(packages);
    }
}
