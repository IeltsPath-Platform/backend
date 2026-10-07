package com.ieltspath.learning.domain.entity;

import java.util.Objects;
import java.util.UUID;

/** One practice package given in a review, answered once; changed only through {@code ReviewItem}. */
public final class ReviewSet {
    private final UUID id;
    private final UUID packageId;
    private final UUID packageVersionId;
    private UUID requestId;
    private Boolean passed;
    private Integer correct;
    private Integer total;

    public ReviewSet(UUID id, UUID packageId, UUID packageVersionId) {
        this.id = Objects.requireNonNull(id, "id");
        this.packageId = Objects.requireNonNull(packageId, "packageId");
        this.packageVersionId = Objects.requireNonNull(packageVersionId, "packageVersionId");
    }

    public UUID id() { return id; }
    public UUID packageId() { return packageId; }
    public UUID packageVersionId() { return packageVersionId; }
    public UUID requestId() { return requestId; }
    public Boolean passed() { return passed; }
    public Integer correct() { return correct; }
    public Integer total() { return total; }
    public boolean isOpen() { return passed == null; }

    public void close(UUID requestId, int correct, int total, boolean passed) {
        if (!isOpen()) throw new IllegalStateException("Review set is already answered");
        this.requestId = Objects.requireNonNull(requestId, "requestId");
        this.correct = correct;
        this.total = total;
        this.passed = passed;
    }
}
