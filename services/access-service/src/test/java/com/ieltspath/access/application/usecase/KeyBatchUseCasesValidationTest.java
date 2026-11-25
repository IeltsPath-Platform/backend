package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.exception.KeyDistributionException;
import com.ieltspath.access.application.port.KeyCodeCipher;
import com.ieltspath.access.application.port.KeyDistributionStore;
import com.ieltspath.access.application.port.KeyDistributionStore.Allocation;
import com.ieltspath.access.application.port.SaleDirectory;
import com.ieltspath.access.application.port.SaleDirectory.SaleAccount;
import com.ieltspath.access.application.service.KeyCodeReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeyBatchUseCasesValidationTest {

    @Mock
    private KeyDistributionStore store;
    @Mock
    private KeyCodeCipher cipher;
    @Mock
    private SaleDirectory directory;

    private KeyBatchUseCases useCases;
    private final UUID manager = UUID.randomUUID();
    private final UUID product = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCases = new KeyBatchUseCases(store, cipher, new KeyCodeReader(cipher), directory);
    }

    @Test
    @DisplayName("A batch must hold between one and a thousand keys")
    void quantityOutsideTheRangeIsRefused() {
        assertThatThrownBy(() -> useCases.createBatch(manager, product, 0, null, null))
                .isInstanceOf(KeyDistributionException.class).hasMessageContaining("từ 1 đến 1000");
        assertThatThrownBy(() -> useCases.createBatch(manager, product, 1001, null, null))
                .isInstanceOf(KeyDistributionException.class).hasMessageContaining("từ 1 đến 1000");
        verify(store, never()).createBatch(any(), any(), anyInt(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("A batch cannot expire on a day that has already passed")
    void expiryInThePastIsRefused() {
        LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1);

        assertThatThrownBy(() -> useCases.createBatch(manager, product, 5, yesterday, null))
                .isInstanceOf(KeyDistributionException.class).hasMessageContaining("quá khứ");
    }

    @Test
    @DisplayName("Keys cannot be given to someone who is not an active sale, and nothing moves")
    void allocationToAnInactiveSaleIsRefusedBeforeAnyKeyMoves() {
        UUID lockedSale = UUID.randomUUID();
        when(directory.sales()).thenReturn(List.of(new SaleAccount(lockedSale, "Sale khóa", false)));

        assertThatThrownBy(() -> useCases.allocate(manager, "B-2611-001", List.of(new Allocation(lockedSale, 3))))
                .isInstanceOf(KeyDistributionException.class)
                .satisfies(error -> assertThat(((KeyDistributionException) error).code()).isEqualTo("SALE_NOT_ELIGIBLE"));
        verify(store, never()).allocate(any(), anyString(), anyList());
    }

    @Test
    @DisplayName("Two lines for the same sale are added together before the keys move")
    void repeatedSaleLinesAreMerged() {
        UUID sale = UUID.randomUUID();
        when(directory.sales()).thenReturn(List.of(new SaleAccount(sale, "Sale A", true)));

        useCases.allocate(manager, "B-2611-001", List.of(new Allocation(sale, 2), new Allocation(sale, 3)));

        verify(store).allocate(any(), anyString(), org.mockito.ArgumentMatchers.argThat(lines ->
                lines.size() == 1 && lines.get(0).quantity() == 5));
    }

    @Test
    @DisplayName("An empty revoke note is replaced by the default note, so every revoke says who did it")
    void revokeWithoutNoteGetsTheDefaultNote() {
        useCases.revoke(manager, List.of(UUID.randomUUID()), "  ");

        verify(store).revoke(any(), anyList(), org.mockito.ArgumentMatchers.eq("Thu hồi bởi Quản lý bán hàng"));
    }

}
