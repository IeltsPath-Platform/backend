package com.ieltspath.access.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class KeyDistributionRequests {
    private KeyDistributionRequests() {
    }

    public record CreateBatch(@NotNull(message = "Cần chọn sản phẩm") UUID productId,
                              int quantity,
                              LocalDate expiresOn,
                              @Size(max = 100, message = "Tên đợt không được dài quá 100 ký tự") String label) {}

    public record Allocate(@NotEmpty(message = "Cần chọn ít nhất một nhân viên") List<@Valid Line> allocations) {
        public record Line(@NotNull(message = "Cần chọn nhân viên") UUID saleId, int quantity) {}
    }

    public record Reassign(@NotEmpty(message = "Cần chọn ít nhất một key") List<UUID> keyIds,
                           @NotNull(message = "Cần chọn nhân viên nhận key") UUID toSaleId) {}

    public record Keys(@NotEmpty(message = "Cần chọn ít nhất một key") List<UUID> keyIds) {}

    public record Revoke(@NotEmpty(message = "Cần chọn ít nhất một key") List<UUID> keyIds,
                         @Size(max = 500, message = "Ghi chú không được dài quá 500 ký tự") String note) {}

    public record RevokeUnused(@Size(max = 500, message = "Ghi chú không được dài quá 500 ký tự") String note) {}

    public record Issue(@NotNull(message = "Cần chọn sản phẩm") UUID productId) {}
}
