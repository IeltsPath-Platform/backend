package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.query.PageQuery;
import com.ieltspath.user.domain.aggregate.AuthAuditEvent;
import com.ieltspath.user.domain.repository.AuthAuditEventRepository;
import com.ieltspath.user.domain.vo.OwnedPage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class GetAuthAuditEventsUseCaseTest {
    private final AuthAuditEventRepository repository = mock(AuthAuditEventRepository.class);
    private final GetAuthAuditEventsUseCase useCase = new GetAuthAuditEventsUseCase(repository);

    @Test
    void pagesUserEventsByNewestAndMasksTheFinalIpOctet() {
        UUID userId = UUID.randomUUID();
        var entity = new AuthAuditEvent(UUID.randomUUID(), userId, "SESSION_REVOKED", "203.0.113.45", "Browser",
                "req-1", Map.of("reason", "USER_REQUEST"), LocalDateTime.now());
        when(repository.findByUserId(userId, null, 0, 100)).thenReturn(new OwnedPage<>(List.of(entity), 1));

        var result = useCase.execute(userId, new PageQuery(0, 100), null);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).ipAddress()).isEqualTo("203.0.113.*");
        assertThat(result.size()).isEqualTo(100);
    }

    @Test
    void rejectsUnknownEventTypeBeforeQuerying() {
        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(), new PageQuery(0, 20), "PASSWORD"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Loại sự kiện bảo mật không hợp lệ.");
        verifyNoInteractions(repository);
    }
}
