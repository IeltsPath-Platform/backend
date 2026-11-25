package com.ieltspath.access.application.port;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Every read and write of key batches, allocations and handing out. Each method that changes keys is one transaction,
 * so a failure leaves nothing half done, and takes the locks it needs inside it. Nothing here calls another service.
 *
 * <p>A key's state is derived, not stored: READY (made, not handed to a student), ISSUED (handed out, not yet used),
 * ACTIVATED, REVOKED, EXPIRED. Where a READY key sits comes from its holder: a sale id, or none for the shared pool.
 */
public interface KeyDistributionStore {

    /** The states a key can show; the order is the order screens list them in. */
    enum State { READY, ISSUED, ACTIVATED, REVOKED, EXPIRED }

    /** One new key as the store writes it: the hash activation compares, the last characters, and the encrypted full code. */
    record NewKey(String codeHash, String codeHint, String ciphertext) {}

    record Batch(String code, String label, UUID productId, String productName, int quantity, LocalDate expiresOn,
                 Instant createdAt, UUID createdBy, Map<State, Integer> byState, int pool) {}

    record KeyRow(UUID id, String batchCode, int seq, UUID productId, String productName, String codeHint, State state,
                  UUID saleId, Instant issuedAt, Instant redeemedAt, Instant revokedAt, Instant expiresAt, String note,
                  boolean codeStored) {}

    record Page<T>(List<T> items, long total, int page, int size) {}

    /**
     * Every field is optional. {@code pool} limits to keys no sale holds and {@code held} to keys a sale holds; {@code issuedOnly}
     * to keys already handed out; {@code newestFirst} lists the most recently issued first instead of by sequence number.
     * {@code hint} matches the last characters of the code, the only part of it that is searchable.
     */
    record KeyFilter(String batchCode, State state, UUID saleId, boolean pool, boolean held, UUID productId, String hint,
                     boolean issuedOnly, boolean newestFirst) {}

    /** Counts of one batch or product or sale by state, for the manager's overview. */
    record Counts(String key, String label, Map<State, Integer> byState) {}

    record SaleHolding(UUID saleId, int held, int issued, int activated) {}

    record IssuedKey(UUID keyId, String ciphertext, String codeHint, String batchCode) {}

    record AllocationResult(int moved, int skipped) {}

    /** Names the sale accounts so a key row can show who holds it; filled by the use case from the user service. */
    record Allocation(UUID saleId, int quantity) {}

    // ---- manager: batches -------------------------------------------------------------------------------------

    /**
     * Makes the batch and all its keys in one transaction: next code of the month, then {@code quantity} keys numbered from 1.
     * A code that is already taken is replaced by asking {@code codes} for another, so the batch always ends with exactly
     * {@code quantity} keys. Returns the batch code.
     */
    String createBatch(UUID actor, UUID productId, int quantity, LocalDate expiresOn, Instant expiresAt, String label,
                       Supplier<NewKey> codes);

    boolean productExists(UUID productId);

    Page<Batch> listBatches(UUID productId, String month, int page, int size);

    java.util.Optional<Batch> findBatch(String code);

    Page<KeyRow> listKeys(KeyFilter filter, int page, int size);

    /** Counts by batch, by product and by sale, each in one query, for the month given as {@code YYYY-MM} (or all time). */
    List<Counts> countsByBatch(String month);

    List<Counts> countsByProduct(String month);

    List<SaleHolding> holdings();

    // ---- manager: moving and removing --------------------------------------------------------------------------

    /** Moves READY keys of a batch from the shared pool to the sales, lowest sequence first. All or nothing. */
    void allocate(UUID actor, String batchCode, List<Allocation> allocations);

    /** Moves READY keys the given sale or pool holds to {@code toSale}; others are skipped. */
    AllocationResult reassign(UUID actor, List<UUID> keyIds, UUID toSale);

    /** Takes READY keys held by a sale back to the shared pool; others are skipped. */
    AllocationResult withdraw(UUID actor, List<UUID> keyIds);

    /** Revokes keys that are still unused (READY or ISSUED); redeemed, revoked and unknown keys are skipped. */
    AllocationResult revoke(UUID actor, List<UUID> keyIds, String note);

    AllocationResult revokeUnusedInBatch(UUID actor, String batchCode, String note);

    // ---- handing out -------------------------------------------------------------------------------------------

    /**
     * Takes the next key of a product for {@code holder} (a sale id, or null for the shared pool) and marks it issued.
     * The same {@code idempotencyKey} from the same actor and product returns the key taken the first time.
     */
    IssuedKey issue(UUID actor, UUID holder, UUID productId, String idempotencyKey);

    /** How many READY keys a sale can still hand out, per product, and per batch for the chart. */
    List<Counts> saleStockByProduct(UUID saleId);

    List<Counts> saleStockByBatch(UUID saleId);

    // ---- one key -----------------------------------------------------------------------------------------------

    java.util.Optional<KeyRow> findKey(UUID keyId);

    /** The encrypted code of a key, or empty when there is no such key; the caller checks who may see it. */
    java.util.Optional<String> ciphertextOf(UUID keyId);

    /** Writes one audit row for a reveal; never holds the code. */
    void auditReveal(UUID actor, UUID keyId);

    /** Every key of a batch with its encrypted code, for an export, and writes the audit row in the same transaction. */
    List<ExportRow> exportBatch(UUID actor, String batchCode);

    record ExportRow(int seq, String ciphertext, String codeHint, State state, UUID saleId, Instant issuedAt,
                     Instant redeemedAt, Instant expiresAt, String productName) {}
}
