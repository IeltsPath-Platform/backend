package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.exception.KeyDistributionException;
import com.ieltspath.access.application.port.KeyCodeCipher;
import com.ieltspath.access.application.port.KeyDistributionStore;
import com.ieltspath.access.application.port.KeyDistributionStore.Allocation;
import com.ieltspath.access.application.port.KeyDistributionStore.AllocationResult;
import com.ieltspath.access.application.port.KeyDistributionStore.Batch;
import com.ieltspath.access.application.port.KeyDistributionStore.Counts;
import com.ieltspath.access.application.port.KeyDistributionStore.ExportRow;
import com.ieltspath.access.application.port.KeyDistributionStore.IssuedKey;
import com.ieltspath.access.application.port.KeyDistributionStore.KeyFilter;
import com.ieltspath.access.application.port.KeyDistributionStore.KeyRow;
import com.ieltspath.access.application.port.KeyDistributionStore.NewKey;
import com.ieltspath.access.application.port.KeyDistributionStore.Page;
import com.ieltspath.access.application.port.KeyDistributionStore.SaleHolding;
import com.ieltspath.access.application.port.KeyDistributionStore.State;
import com.ieltspath.access.application.port.SaleDirectory;
import com.ieltspath.access.application.port.SaleDirectory.SaleAccount;
import com.ieltspath.access.application.service.KeyCodeReader;
import com.ieltspath.access.application.service.KeyIdempotency;
import com.ieltspath.access.domain.exception.KeyProductNotFoundException;
import com.ieltspath.access.domain.vo.ActivationKeyCode;
import com.ieltspath.commonresilience.DownstreamUnavailableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * What a sales manager can do with key batches. Calls to the user service happen here, before the store opens its
 * transaction, never inside it.
 */
@Service
public class KeyBatchUseCases {
    static final int MAX_BATCH_QUANTITY = 1000;
    static final int MAX_KEYS_PER_ACTION = 1000;
    static final int MAX_PAGE_SIZE = 100;
    static final int MAX_ALLOCATION_LINES = 100;
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Pattern MONTH = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])");
    private static final String DEFAULT_REVOKE_NOTE = "Thu hồi bởi Quản lý bán hàng";

    private final KeyDistributionStore store;
    private final KeyCodeCipher cipher;
    private final KeyCodeReader reader;
    private final SaleDirectory directory;

    public KeyBatchUseCases(KeyDistributionStore store, KeyCodeCipher cipher, KeyCodeReader reader, SaleDirectory directory) {
        this.store = store;
        this.cipher = cipher;
        this.reader = reader;
        this.directory = directory;
    }

    // ---- batches ----------------------------------------------------------------------------------------------

    public Batch createBatch(UUID actor, UUID productId, int quantity, LocalDate expiresOn, String label) {
        if (quantity < 1 || quantity > MAX_BATCH_QUANTITY) {
            throw badRequest("Số lượng key phải từ 1 đến " + MAX_BATCH_QUANTITY + ".");
        }
        if (expiresOn != null && expiresOn.isBefore(LocalDate.now(ZONE))) {
            throw badRequest("Hạn dùng của đợt không được ở trong quá khứ.");
        }
        if (label != null && label.strip().length() > 100) throw badRequest("Tên đợt không được dài quá 100 ký tự.");
        if (!store.productExists(productId)) throw new KeyProductNotFoundException(productId);
        reader.requireAvailable();
        Instant expiresAt = expiresOn == null ? null : expiresOn.atTime(23, 59, 59, 999_000_000).atZone(ZONE).toInstant();
        String code = store.createBatch(actor, productId, quantity, expiresOn, expiresAt, label, this::newKey);
        return store.findBatch(code).orElseThrow(KeyDistributionException::batchNotFound);
    }

    private NewKey newKey() {
        String raw = ActivationKeyCode.generateRaw();
        ActivationKeyCode code = ActivationKeyCode.fromRawKey(raw);
        return new NewKey(code.codeHash(), code.codeHint(), cipher.encrypt(raw));
    }

    public Page<Batch> listBatches(UUID productId, String month, int page, int size) {
        return store.listBatches(productId, requireMonth(month), Math.max(page, 0), clampSize(size));
    }

    public Batch batch(String code) {
        return store.findBatch(code).orElseThrow(KeyDistributionException::batchNotFound);
    }

    public Page<KeyRow> batchKeys(String batchCode, State state, UUID saleId, boolean pool, boolean held, String hint, int page, int size) {
        batch(batchCode);
        return store.listKeys(new KeyFilter(batchCode, state, saleId, pool, held, null, hint, false, false),
                Math.max(page, 0), clampSize(size));
    }

    public Overview overview(String month) {
        String checked = requireMonth(month);
        return new Overview(store.countsByBatch(checked), store.countsByProduct(checked));
    }

    public record Overview(List<Counts> byBatch, List<Counts> byProduct) {}

    /** The sales accounts with what each holds; fails with a retryable error when the user service cannot answer. */
    public List<SaleView> sales() {
        Map<UUID, SaleHolding> holdings = new HashMap<>();
        store.holdings().forEach(holding -> holdings.put(holding.saleId(), holding));
        return directory.sales().stream().map(account -> {
            SaleHolding held = holdings.get(account.id());
            return new SaleView(account.id(), account.displayName(), account.active(),
                    held == null ? 0 : held.held(), held == null ? 0 : held.issued(), held == null ? 0 : held.activated());
        }).toList();
    }

    public record SaleView(UUID id, String displayName, boolean active, int held, int issued, int activated) {}

    // ---- moving and removing ----------------------------------------------------------------------------------

    public void allocate(UUID actor, String batchCode, List<Allocation> requested) {
        if (requested == null || requested.isEmpty() || requested.size() > MAX_ALLOCATION_LINES) {
            throw badRequest("Cần chọn từ 1 đến " + MAX_ALLOCATION_LINES + " nhân viên bán hàng.");
        }
        Map<UUID, Integer> merged = new LinkedHashMap<>();
        for (Allocation line : requested) {
            if (line.saleId() == null || line.quantity() < 1 || line.quantity() > MAX_BATCH_QUANTITY) {
                throw badRequest("Số key giao cho mỗi nhân viên phải từ 1 đến " + MAX_BATCH_QUANTITY + ".");
            }
            merged.merge(line.saleId(), line.quantity(), Integer::sum);
        }
        requireEligible(merged.keySet());
        store.allocate(actor, batchCode, merged.entrySet().stream().map(e -> new Allocation(e.getKey(), e.getValue())).toList());
    }

    public AllocationResult reassign(UUID actor, List<UUID> keyIds, UUID toSale) {
        requireKeyIds(keyIds);
        if (toSale == null) throw badRequest("Cần chọn nhân viên nhận key.");
        requireEligible(List.of(toSale));
        return store.reassign(actor, keyIds, toSale);
    }

    public AllocationResult withdraw(UUID actor, List<UUID> keyIds) {
        requireKeyIds(keyIds);
        return store.withdraw(actor, keyIds);
    }

    public AllocationResult revoke(UUID actor, List<UUID> keyIds, String note) {
        requireKeyIds(keyIds);
        return store.revoke(actor, keyIds, noteOrDefault(note));
    }

    public AllocationResult revokeUnusedInBatch(UUID actor, String batchCode, String note) {
        return store.revokeUnusedInBatch(actor, batchCode, noteOrDefault(note));
    }

    // ---- looking at codes -------------------------------------------------------------------------------------

    /** The full code of any batch key, for a manager; the look is written to the audit log. */
    @Transactional
    public String reveal(UUID actor, UUID keyId) {
        KeyRow key = store.findKey(keyId).filter(row -> row.batchCode() != null).orElseThrow(KeyDistributionException::keyNotFound);
        String code = reader.read(store.ciphertextOf(key.id()).orElse(null));
        store.auditReveal(actor, key.id());
        return code;
    }

    /** The CSV text of a batch, UTF-8 without the byte-order mark (the controller adds it). */
    public String exportCsv(UUID actor, String batchCode) {
        reader.requireAvailable();
        List<ExportRow> rows = store.exportBatch(actor, batchCode);
        Map<UUID, String> names = saleNames();
        StringBuilder csv = new StringBuilder("Mã key,Sản phẩm,Trạng thái,Sale,Cấp lúc,Kích hoạt lúc,Hạn dùng\r\n");
        for (ExportRow row : rows) {
            csv.append(cell(reader.readOrBlank(row.ciphertext()))).append(',')
                    .append(cell(row.productName())).append(',')
                    .append(cell(stateLabel(row.state()))).append(',')
                    .append(cell(row.saleId() == null ? "" : names.getOrDefault(row.saleId(), ""))).append(',')
                    .append(cell(time(row.issuedAt()))).append(',')
                    .append(cell(time(row.redeemedAt()))).append(',')
                    .append(cell(time(row.expiresAt()))).append("\r\n");
        }
        return csv.toString();
    }

    /** Names for the export when the user service answers; without them the Sale column stays empty. */
    private Map<UUID, String> saleNames() {
        try {
            Map<UUID, String> names = new HashMap<>();
            directory.sales().forEach(account -> names.put(account.id(), account.displayName()));
            return names;
        } catch (DownstreamUnavailableException unavailable) {
            return Map.of();
        }
    }

    // ---- handing out directly ---------------------------------------------------------------------------------

    /** A manager hands a key out of the shared pool, the same way a sale hands out one it holds. */
    @Transactional
    public IssuedCode issueDirect(UUID actor, UUID productId, String idempotencyKey) {
        KeyIdempotency.require(idempotencyKey);
        reader.requireAvailable();
        if (!store.productExists(productId)) throw new KeyProductNotFoundException(productId);
        IssuedKey issued = store.issue(actor, null, productId, idempotencyKey);
        return new IssuedCode(issued.keyId(), reader.read(issued.ciphertext()), issued.batchCode(),
                store.findKey(issued.keyId()).orElseThrow(KeyDistributionException::keyNotFound));
    }

    public record IssuedCode(UUID keyId, String code, String batchCode, KeyRow key) {}

    // ---- helpers ----------------------------------------------------------------------------------------------

    private void requireEligible(Iterable<UUID> saleIds) {
        Map<UUID, SaleAccount> accounts = new HashMap<>();
        directory.sales().forEach(account -> accounts.put(account.id(), account));
        for (UUID id : saleIds) {
            SaleAccount account = accounts.get(id);
            if (account == null || !account.active()) throw KeyDistributionException.saleNotEligible();
        }
    }

    private static void requireKeyIds(List<UUID> keyIds) {
        if (keyIds == null || keyIds.isEmpty() || keyIds.size() > MAX_KEYS_PER_ACTION || keyIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw badRequest("Cần chọn từ 1 đến " + MAX_KEYS_PER_ACTION + " key.");
        }
    }

    private static String noteOrDefault(String note) {
        String value = note == null ? "" : note.strip();
        if (value.length() > 500) throw badRequest("Ghi chú không được dài quá 500 ký tự.");
        return value.isEmpty() ? DEFAULT_REVOKE_NOTE : value;
    }

    private static String requireMonth(String month) {
        if (month == null || month.isBlank()) return null;
        if (!MONTH.matcher(month).matches()) throw badRequest("Tháng phải có dạng năm-tháng, ví dụ 2026-11.");
        return month;
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private static KeyDistributionException badRequest(String message) {
        return new KeyDistributionException(org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
    }

    private static String stateLabel(State state) {
        return switch (state) {
            case READY -> "Sẵn sàng";
            case ISSUED -> "Đã cấp";
            case ACTIVATED -> "Đã kích hoạt";
            case REVOKED -> "Thu hồi";
            case EXPIRED -> "Hết hạn";
        };
    }

    private static String time(Instant instant) {
        return instant == null ? "" : java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZONE).format(instant);
    }

    /** Quoted; a cell that starts with = + - or @ gets a leading space so a spreadsheet does not read it as a formula. */
    private static String cell(String value) {
        String text = value == null ? "" : value;
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) text = " " + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

}
