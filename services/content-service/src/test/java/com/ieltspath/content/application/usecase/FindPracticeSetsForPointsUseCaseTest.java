package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.UnlinkedPracticeSetReader;
import com.ieltspath.content.application.port.UnlinkedPracticeSetReader.PracticeSetRef;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FindPracticeSetsForPointsUseCaseTest {

    private final UnlinkedPracticeSetReader reader = mock(UnlinkedPracticeSetReader.class);
    private final FindPracticeSetsForPointsUseCase useCase = new FindPracticeSetsForPointsUseCase(reader);

    @Test
    void listsEachSetOnceEvenWhenItMeasuresSeveralPoints() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        PracticeSetRef shared = new PracticeSetRef(UUID.randomUUID(), "PS_SHARED");
        PracticeSetRef only = new PracticeSetRef(UUID.randomUUID(), "PS_ONLY");
        Map<UUID, List<PracticeSetRef>> found = new LinkedHashMap<>();
        found.put(first, List.of(shared));
        found.put(second, List.of(shared, only));
        when(reader.practiceSets(List.of(first, second), Integer.MAX_VALUE)).thenReturn(found);

        assertThat(useCase.execute(List.of(first, second))).containsExactly(shared.packageId(), only.packageId());
    }

    @Test
    void noPointsIsNoSetsWithoutAskingTheReader() {
        assertThat(useCase.execute(List.of())).isEmpty();

        verifyNoInteractions(reader);
    }

    @Test
    void refusesMoreThanTheLimitOfPoints() {
        List<UUID> tooMany = IntStream.rangeClosed(0, FindPracticeSetsForPointsUseCase.MAX_POINTS)
                .mapToObj(index -> UUID.randomUUID()).toList();

        assertThatThrownBy(() -> useCase.execute(tooMany)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(reader);
    }
}
