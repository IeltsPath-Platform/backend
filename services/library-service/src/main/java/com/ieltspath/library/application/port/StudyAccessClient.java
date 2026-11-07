package com.ieltspath.library.application.port;

import java.util.UUID;

public interface StudyAccessClient {
    record PointsDebit(UUID ledgerId, long balanceAfter) {}

    PointsDebit debit(UUID userId, long amount, String referenceType, UUID referenceId,
                      String idempotencyKey, String description);

    boolean premium();

    class InsufficientPointsException extends RuntimeException {
        public InsufficientPointsException() { super("Không đủ điểm để thực hiện giao dịch."); }
    }

    class PointsIdempotencyConflictException extends RuntimeException {
        public PointsIdempotencyConflictException() { super("Khóa giao dịch đã được sử dụng với dữ liệu khác."); }
    }

    class AccessUnavailableException extends RuntimeException {
        public AccessUnavailableException() { super("Dịch vụ quyền truy cập tạm thời không khả dụng."); }
        public AccessUnavailableException(Throwable cause) {
            super("Dịch vụ quyền truy cập tạm thời không khả dụng.", cause);
        }
    }
}
