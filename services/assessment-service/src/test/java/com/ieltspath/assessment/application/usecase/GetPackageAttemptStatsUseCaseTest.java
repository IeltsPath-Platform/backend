package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.PackageAttemptStatsReader;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GetPackageAttemptStatsUseCaseTest {
    private final PackageAttemptStatsReader reader = mock(PackageAttemptStatsReader.class);
    private final GetPackageAttemptStatsUseCase useCase = new GetPackageAttemptStatsUseCase(reader);

    @Test
    void noPackagesMeansNoQuery() {
        assertThat(useCase.execute(null)).isEmpty();
        assertThat(useCase.execute(List.of())).isEmpty();
        verifyNoInteractions(reader);
    }

    @Test
    void duplicateIdsAreAskedOnce() {
        UUID id = UUID.randomUUID();
        var stats = new PackageAttemptStatsReader.PackageStats(id, 3, 2, new BigDecimal("6.5"));
        when(reader.forPackages(any())).thenReturn(List.of(stats));

        assertThat(useCase.execute(List.of(id, id))).containsExactly(stats);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UUID>> asked = ArgumentCaptor.forClass(Collection.class);
        verify(reader).forPackages(asked.capture());
        assertThat(asked.getValue()).containsExactly(id);
    }

    @Test
    void moreThanOneHundredPackagesIsRefusedInVietnamese() {
        List<UUID> many = IntStream.range(0, GetPackageAttemptStatsUseCase.MAX_PACKAGES + 1)
                .mapToObj(i -> UUID.randomUUID()).toList();

        assertThatThrownBy(() -> useCase.execute(many)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tối đa 100 đề");
        verifyNoInteractions(reader);
    }
}
