package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.exception.KeyDistributionException;
import com.ieltspath.access.application.port.KeyDistributionStore;
import com.ieltspath.access.application.port.KeyDistributionStore.Counts;
import com.ieltspath.access.application.port.KeyDistributionStore.IssuedKey;
import com.ieltspath.access.application.port.KeyDistributionStore.KeyFilter;
import com.ieltspath.access.application.port.KeyDistributionStore.KeyRow;
import com.ieltspath.access.application.port.KeyDistributionStore.Page;
import com.ieltspath.access.application.port.KeyDistributionStore.State;
import com.ieltspath.access.application.service.KeyCodeReader;
import com.ieltspath.access.application.service.KeyIdempotency;
import com.ieltspath.access.domain.exception.KeyProductNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * What a sales staff member can do: see the keys held for them, hand out the next key of a product, and look at the keys
 * they handed out. Every method works on the caller's own keys only; another sale's key is "not found".
 */
@Service
public class SaleKeyUseCases {
    private static final int MAX_PAGE_SIZE = 100;

    private final KeyDistributionStore store;
    private final KeyCodeReader reader;

    public SaleKeyUseCases(KeyDistributionStore store, KeyCodeReader reader) {
        this.store = store;
        this.reader = reader;
    }

    /** What the sale can still hand out: keys held and not yet issued, per product, plus every state per batch for the chart. */
    public Stock stock(UUID sale) {
        return new Stock(store.saleStockByProduct(sale), store.saleStockByBatch(sale));
    }

    public record Stock(List<Counts> byProduct, List<Counts> byBatch) {}

    /**
     * Takes the next key of the product for this sale and returns its full code. One transaction: when the stored code
     * cannot be read back, the key stays unissued instead of being given out and lost.
     */
    @Transactional
    public IssuedCode issue(UUID sale, UUID productId, String idempotencyKey) {
        KeyIdempotency.require(idempotencyKey);
        reader.requireAvailable();
        if (!store.productExists(productId)) throw new KeyProductNotFoundException(productId);
        IssuedKey issued = store.issue(sale, sale, productId, idempotencyKey);
        return new IssuedCode(issued.keyId(), reader.read(issued.ciphertext()), issued.batchCode(),
                store.findKey(issued.keyId()).orElseThrow(KeyDistributionException::keyNotFound));
    }

    public record IssuedCode(UUID keyId, String code, String batchCode, KeyRow key) {}

    /** Keys this sale handed out, newest first. */
    public Page<KeyRow> issued(UUID sale, UUID productId, State state, String hint, int page, int size) {
        return store.listKeys(new KeyFilter(null, state, sale, false, false, productId, hint, true, true),
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    public KeyRow key(UUID sale, UUID keyId) {
        return own(sale, keyId);
    }

    /** The full code of a key this sale handed out; looking is written to the audit log. */
    @Transactional
    public String reveal(UUID sale, UUID keyId) {
        KeyRow key = own(sale, keyId);
        // Holding a key is not enough: its code is shown at the moment it is handed out, so every key that leaves has an ISSUE row.
        if (key.issuedAt() == null) throw KeyDistributionException.keyNotIssued();
        String code = reader.read(store.ciphertextOf(key.id()).orElse(null));
        store.auditReveal(sale, key.id());
        return code;
    }

    private KeyRow own(UUID sale, UUID keyId) {
        return store.findKey(keyId).filter(key -> sale.equals(key.saleId())).orElseThrow(KeyDistributionException::keyNotFound);
    }
}
