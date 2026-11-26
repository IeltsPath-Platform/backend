package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.repository.LoginHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginHistoryRetentionUseCaseTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 3, 30);

    @Mock
    private LoginHistoryRepository repository;
    @InjectMocks
    private LoginHistoryRetentionUseCase useCase;

    @Test
    void removesOldAttemptsInBatchesAndReportsTheTotal() {
        when(repository.deleteOlderThan(NOW.minusDays(180), 3)).thenReturn(3, 3, 1);

        assertThat(useCase.execute(180, 3, false, NOW)).isEqualTo(7);

        verify(repository, org.mockito.Mockito.times(3)).deleteOlderThan(LocalDateTime.of(2026, 5, 2, 3, 30), 3);
    }

    @Test
    void dryRunCountsEligibleRowsWithoutDeleting() {
        when(repository.countOlderThan(NOW.minusDays(180))).thenReturn(23L);

        assertThat(useCase.execute(180, 1000, true, NOW)).isEqualTo(23L);

        verify(repository).countOlderThan(NOW.minusDays(180));
        verifyNoInteractionsAfterCount();
    }

    @Test
    void refusesInvalidRetentionAndBatchValues() {
        assertThatThrownBy(() -> useCase.execute(0, 1, false, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute(-5, 1, false, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute(180, 0, false, NOW)).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(repository);
    }

    private void verifyNoInteractionsAfterCount() {
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never())
                .deleteOlderThan(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }
}
