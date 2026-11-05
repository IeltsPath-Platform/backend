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
class PurgeLoginHistoryUseCaseTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 3, 30);

    @Mock
    private LoginHistoryRepository repository;
    @InjectMocks
    private PurgeLoginHistoryUseCase useCase;

    @Test
    void removesAttemptsOlderThanTheRetentionPeriodAndReportsTheCount() {
        when(repository.deleteOlderThan(NOW.minusDays(90))).thenReturn(7);

        assertThat(useCase.execute(90, NOW)).isEqualTo(7);

        verify(repository).deleteOlderThan(LocalDateTime.of(2026, 7, 31, 3, 30));
    }

    @Test
    void refusesARetentionPeriodThatWouldEraseEverything() {
        assertThatThrownBy(() -> useCase.execute(0, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute(-5, NOW)).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(repository);
    }
}
