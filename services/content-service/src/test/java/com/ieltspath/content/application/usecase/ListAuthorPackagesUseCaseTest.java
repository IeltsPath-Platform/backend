package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.AuthorPackageReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListAuthorPackagesUseCaseTest {

    private final AuthorPackageReader reader = mock(AuthorPackageReader.class);
    private final ListAuthorPackagesUseCase useCase = new ListAuthorPackagesUseCase(reader);
    private final AuthorPackageReader.Filter none = new AuthorPackageReader.Filter(null, null, null, null, null, null);

    @Test
    void reportsThePageAndHowManyPagesTheTotalNeeds() {
        when(reader.list(any(), anyInt(), anyInt())).thenReturn(new AuthorPackageReader.Page(51, List.of()));

        var page = useCase.execute(none, 1, 25);

        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(25);
        assertThat(page.totalElements()).isEqualTo(51);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    void refusesAPageOrSizeOutsideTheRangeInVietnamese() {
        for (int[] bad : new int[][] {{-1, 25}, {0, 0}, {0, 101}}) {
            assertThatThrownBy(() -> useCase.execute(none, bad[0], bad[1]))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("kích thước trang từ 1 đến 100");
        }
        verifyNoInteractions(reader);
    }
}
