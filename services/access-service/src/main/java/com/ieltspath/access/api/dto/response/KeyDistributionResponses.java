package com.ieltspath.access.api.dto.response;

import com.ieltspath.access.application.port.KeyDistributionStore.AllocationResult;
import com.ieltspath.access.application.port.KeyDistributionStore.Batch;
import com.ieltspath.access.application.port.KeyDistributionStore.Counts;
import com.ieltspath.access.application.port.KeyDistributionStore.KeyRow;
import com.ieltspath.access.application.port.KeyDistributionStore.Page;
import com.ieltspath.access.application.service.KeyCodeReader;
import com.ieltspath.access.application.usecase.KeyBatchUseCases.SaleView;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/** What the sale and manager screens read. A key is always shown with its masked code; the full code has its own endpoint. */
public final class KeyDistributionResponses {
    private KeyDistributionResponses() {
    }

    public record BatchResponse(String code, String label, UUID productId, String productName, int quantity,
                                LocalDate expiresOn, Instant createdAt, UUID createdBy, Map<String, Integer> byState, int pool) {
        public static BatchResponse from(Batch batch) {
            return new BatchResponse(batch.code(), batch.label(), batch.productId(), batch.productName(), batch.quantity(),
                    batch.expiresOn(), batch.createdAt(), batch.createdBy(), states(batch.byState()), batch.pool());
        }
    }

    public record KeyResponse(UUID id, String batchCode, int seq, UUID productId, String productName, String maskedCode,
                              String state, UUID saleId, Instant issuedAt, Instant activatedAt, Instant revokedAt,
                              Instant expiresAt, String note, boolean codeStored) {
        public static KeyResponse from(KeyRow key) {
            return new KeyResponse(key.id(), key.batchCode(), key.seq(), key.productId(), key.productName(),
                    KeyCodeReader.mask(key.codeHint()), key.state().name(), key.saleId(), key.issuedAt(), key.redeemedAt(),
                    key.revokedAt(), key.expiresAt(), key.note(), key.codeStored());
        }
    }

    public record PageResponse<T>(List<T> items, long total, int page, int size) {
        public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> map) {
            return new PageResponse<>(page.items().stream().map(map).toList(), page.total(), page.page(), page.size());
        }
    }

    public record CountsResponse(String key, String label, Map<String, Integer> byState) {
        public static CountsResponse from(Counts counts) {
            return new CountsResponse(counts.key(), counts.label(), states(counts.byState()));
        }
    }

    public record OverviewResponse(List<CountsResponse> byBatch, List<CountsResponse> byProduct) {}

    public record StockResponse(List<CountsResponse> byProduct, List<CountsResponse> byBatch) {}

    public record SaleResponse(UUID id, String displayName, boolean active, int held, int issued, int activated) {
        public static SaleResponse from(SaleView sale) {
            return new SaleResponse(sale.id(), sale.displayName(), sale.active(), sale.held(), sale.issued(), sale.activated());
        }
    }

    /** {@code moved} is how many keys changed; {@code skipped} how many of those asked for were left alone. */
    public record ActionResponse(int changed, int skipped) {
        public static ActionResponse from(AllocationResult result) {
            return new ActionResponse(result.moved(), result.skipped());
        }
    }

    /** The one place a full code is returned besides the reveal endpoint: the moment a key is handed out. */
    public record IssuedKeyResponse(UUID keyId, String code, String batchCode, KeyResponse key) {}

    public record CodeResponse(String code) {}

    private static Map<String, Integer> states(Map<? extends Enum<?>, Integer> counts) {
        Map<String, Integer> out = new LinkedHashMap<>();
        counts.forEach((state, value) -> out.put(state.name(), value));
        return out;
    }
}
