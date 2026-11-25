package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

class ListMySessionsUseCaseTest {
    @Test
    void limitsSessionHistoryToFiftyRowsAtTheRepository() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        UUID userId = UUID.randomUUID();
        when(repository.findActiveSessions(eq(userId), any(LocalDateTime.class), eq(50))).thenReturn(List.of());

        new ListMySessionsUseCase(repository).execute(userId);

        verify(repository).findActiveSessions(eq(userId), any(LocalDateTime.class), eq(50));
    }
}
