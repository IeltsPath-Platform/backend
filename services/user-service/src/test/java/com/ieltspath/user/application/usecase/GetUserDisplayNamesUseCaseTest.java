package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.port.UserDisplayNameReader;
import com.ieltspath.user.application.result.UserDisplayNameResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetUserDisplayNamesUseCaseTest {
    private final UserDisplayNameReader reader = mock(UserDisplayNameReader.class);
    private final GetUserDisplayNamesUseCase useCase = new GetUserDisplayNamesUseCase(reader);

    @Test
    void preservesRequestedOrderDeduplicatesIdsAndOmitsUnknownUsers() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        when(reader.findDisplayNamesByIds(List.of(second, first, missing))).thenReturn(Map.of(
                first, "First Learner", second, "Second Learner"));

        List<UserDisplayNameResult> result = useCase.execute(List.of(second, first, missing, second));

        assertThat(result).containsExactly(new UserDisplayNameResult(second, "Second Learner"),
                new UserDisplayNameResult(first, "First Learner"));
        verify(reader).findDisplayNamesByIds(List.of(second, first, missing));
    }

    @Test
    void rejectsEmptyOrOversizedBatches() {
        assertThatThrownBy(() -> useCase.execute(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute(java.util.stream.IntStream.range(0, 51)
                .mapToObj(ignored -> UUID.randomUUID()).toList())).isInstanceOf(IllegalArgumentException.class);
    }
}
