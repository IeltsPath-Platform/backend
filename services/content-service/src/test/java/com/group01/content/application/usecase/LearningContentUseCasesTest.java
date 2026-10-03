package com.group01.content.application.usecase;

import com.group01.content.application.command.CountPracticeSetsCommand;
import com.group01.content.application.command.CreateContentPackageCommand;
import com.group01.content.application.command.SearchPracticeSetsCommand;
import com.group01.content.application.port.LearningContentReader;
import com.group01.content.domain.vo.MediaReferencePolicy;
import com.group01.content.domain.exception.InvalidPackageLessonException;
import com.group01.content.domain.exception.LessonNotFoundException;
import com.group01.content.domain.exception.PackageVersionNotFoundException;
import com.group01.content.domain.exception.TopicNotFoundException;
import com.group01.content.domain.repository.ContentPackageRepository;
import com.group01.content.domain.vo.PackageType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class LearningContentUseCasesTest {
    private final LearningContentReader reader = mock(LearningContentReader.class);

    @Test
    void practiceSetSearchAppliesDefaults() {
        UUID kp = UUID.randomUUID();
        new SearchPracticeSetsUseCase(reader).execute(new SearchPracticeSetsCommand(kp, null, null, null, null));
        verify(reader).searchPracticeSets(kp, List.of(), 3, 1, null);
    }

    @Test
    void practiceSetSearchPassesExclusionsAndBounds() {
        UUID kp = UUID.randomUUID();
        List<UUID> exclude = List.of(UUID.randomUUID());
        UUID lesson = UUID.randomUUID();
        new SearchPracticeSetsUseCase(reader).execute(new SearchPracticeSetsCommand(kp, exclude, 4, 10, lesson));
        verify(reader).searchPracticeSets(kp, exclude, 4, 10, lesson);
    }

    @Test
    void practiceSetSearchRejectsInvalidInputWithoutQuerying() {
        SearchPracticeSetsUseCase useCase = new SearchPracticeSetsUseCase(reader);
        UUID kp = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new SearchPracticeSetsCommand(null, null, null, null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new SearchPracticeSetsCommand(kp, null, null, 11, null)));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new SearchPracticeSetsCommand(kp, null, 0, null, null)));
        verify(reader, never()).searchPracticeSets(any(), any(), anyInt(), anyInt(), any());
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
        assertThrows(IllegalArgumentException.class, () -> new CreateContentPackageUseCase(packages, reader).execute(
                new CreateContentPackageCommand("X9", "Test", PackageType.TOPIC_TEST, null, null)));
        verifyNoInteractions(packages);
    }

    @Test
    void unknownLessonOrTopicPracticeIsNotFound() {
        UUID id = UUID.randomUUID();
        when(reader.publishedLessonExists(id)).thenReturn(false);
        when(reader.activeTopicExists(id)).thenReturn(false);

        assertThrows(LessonNotFoundException.class, () -> new GetLessonPracticeSetsUseCase(reader).execute(id));
        assertThrows(TopicNotFoundException.class, () -> new GetTopicPracticeSetsUseCase(reader).execute(id));
        verify(reader, never()).lessonPracticeSets(id);
        verify(reader, never()).topicPracticeSets(id);
    }

    @Test
    void availabilityAppliesDefaultsDeduplicatesAndRejectsInvalidInput() {
        CountAvailablePracticeSetsUseCase useCase = new CountAvailablePracticeSetsUseCase(reader);
        UUID kp = UUID.randomUUID();

        useCase.execute(new CountPracticeSetsCommand(List.of(kp, kp), null, null));
        verify(reader).countEligiblePracticeSets(List.of(kp), List.of(), 3);

        List<UUID> tooMany = java.util.stream.Stream.generate(UUID::randomUUID).limit(51).toList();
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new CountPracticeSetsCommand(tooMany, null, null)));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new CountPracticeSetsCommand(List.of(), null, null)));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new CountPracticeSetsCommand(List.of(kp), null, 0)));
        verify(reader, times(1)).countEligiblePracticeSets(any(), any(), anyInt());
    }

    @Test
    void aPackageCanJoinOnlyAnExistingLessonAndOnlyAsAPracticeSet() {
        ContentPackageRepository packages = mock(ContentPackageRepository.class);
        UUID lesson = UUID.randomUUID();
        CreateContentPackageUseCase useCase = new CreateContentPackageUseCase(packages, reader);
        when(reader.lessonExists(lesson)).thenReturn(false);
        assertThrows(InvalidPackageLessonException.class, () -> useCase.execute(
                new CreateContentPackageCommand("PS-NEW", "Practice", PackageType.PRACTICE_SET, null, lesson)));

        when(reader.lessonExists(lesson)).thenReturn(true);
        assertThrows(InvalidPackageLessonException.class, () -> useCase.execute(
                new CreateContentPackageCommand("Q-NEW", "Quiz", PackageType.QUIZ, null, lesson)));
        verify(packages, never()).save(any());

        when(packages.save(any())).thenAnswer(call -> call.getArgument(0));
        assertEquals(lesson, useCase.execute(new CreateContentPackageCommand("PS-NEW", "Practice",
                PackageType.PRACTICE_SET, null, lesson)).lessonId());
    }
}
